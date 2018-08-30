/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.sun.jdi.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.state.IncompatibleClassTracker
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.debugger.FileRankingCalculator.Ranking.Companion.LOW
import org.jetbrains.kotlin.idea.debugger.FileRankingCalculator.Ranking.Companion.MAJOR
import org.jetbrains.kotlin.idea.debugger.FileRankingCalculator.Ranking.Companion.MINOR
import org.jetbrains.kotlin.idea.debugger.FileRankingCalculator.Ranking.Companion.NORMAL
import org.jetbrains.kotlin.idea.debugger.FileRankingCalculator.Ranking.Companion.ZERO
import org.jetbrains.kotlin.idea.refactoring.getLineStartOffset
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes2
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes3
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.varargParameterPosition
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.utils.keysToMap
import kotlin.jvm.internal.FunctionBase
import org.jetbrains.org.objectweb.asm.Type as AsmType

object FileRankingCalculatorForIde : FileRankingCalculator() {
    override fun analyze(element: KtElement) = element.analyze(BodyResolveMode.PARTIAL)
}

abstract class FileRankingCalculator(
    private val checkClassFqName: Boolean = true,
    private val strictMode: Boolean = false
) {
    abstract fun analyze(element: KtElement): BindingContext

    fun findMostAppropriateSource(files: Collection<KtFile>, location: Location): KtFile {
        assert(files.isNotEmpty())

        val fileWithRankings = files.keysToMap { fileRankingSafe(it, location) }
        val fileWithMaxScore = fileWithRankings.maxBy { it.value }!!

        if (strictMode) {
            require(fileWithMaxScore.value.value >= 0) { "Max score is negative" }

            // Allow only one element with max ranking
            require(fileWithRankings.count { it.value == fileWithMaxScore.value } == 1) {
                "Score is the same for several files"
            }
        }

        return fileWithMaxScore.key
    }

    private class Ranking(val value: Int) : Comparable<Ranking> {
        companion object {
            val LOW = Ranking(-1000)
            val ZERO = Ranking(0)
            val MINOR = Ranking(1)
            val NORMAL = Ranking(5)
            val MAJOR = Ranking(10)

            fun minor(condition: Boolean) = if (condition) MINOR else ZERO
        }

        operator fun unaryMinus() = Ranking(-value)
        operator fun plus(other: Ranking) = Ranking(value + other.value)
        override fun compareTo(other: Ranking) = this.value - other.value
        override fun toString() = value.toString()
    }

    private fun collect(vararg conditions: Any): Ranking {
        return conditions
            .map { condition ->
                when (condition) {
                    is Boolean -> Ranking.minor(condition)
                    is Int -> Ranking(condition)
                    is Ranking -> condition
                    else -> error("Invalid condition type ${condition.javaClass.name}")
                }
            }.fold(ZERO) { sum, r -> sum + r }
    }

    private fun rankingForClass(clazz: KtClassOrObject, fqName: String, virtualMachine: VirtualMachine): Ranking {
        val bindingContext = analyze(clazz)
        val descriptor = bindingContext[BindingContext.CLASS, clazz] ?: return ZERO

        val jdiType = virtualMachine.classesByName(fqName).firstOrNull() ?: run {
            // Check at least the class name if not found
            return rankingForClassName(fqName, descriptor, bindingContext)
        }

        return rankingForClass(clazz, jdiType)
    }

    private fun rankingForClass(clazz: KtClassOrObject, type: ReferenceType): Ranking {
        val bindingContext = analyze(clazz)
        val descriptor = bindingContext[BindingContext.CLASS, clazz] ?: return ZERO

        return collect(
            rankingForClassName(type.name(), descriptor, bindingContext),
            Ranking.minor(type.isAbstract && descriptor.modality == Modality.ABSTRACT),
            Ranking.minor(type.isFinal && descriptor.modality == Modality.FINAL),
            Ranking.minor(type.isStatic && !descriptor.isInner),
            rankingForVisibility(descriptor, type)
        )
    }

    private fun rankingForClassName(fqName: String, descriptor: ClassDescriptor, bindingContext: BindingContext): Ranking {
        val expectedFqName = makeTypeMapper(bindingContext).mapType(descriptor).className
        return when {
            checkClassFqName -> if (expectedFqName == fqName) MAJOR else LOW
            else -> if (expectedFqName.simpleName() == fqName.simpleName()) MAJOR else LOW
        }
    }

    private fun rankingForMethod(function: KtFunction, method: Method): Ranking {
        val bindingContext = analyze(function)
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, function] as? CallableMemberDescriptor ?: return ZERO

        if (function !is KtConstructor<*> && method.name() != descriptor.name.asString())
            return LOW

        val typeMapper = makeTypeMapper(bindingContext)

        return collect(
            method.isConstructor && function is KtConstructor<*>,
            method.isAbstract && descriptor.modality == Modality.ABSTRACT,
            method.isFinal && descriptor.modality == Modality.FINAL,
            method.isVarArgs && descriptor.varargParameterPosition() >= 0,
            rankingForVisibility(descriptor, method),
            descriptor.valueParameters.size == (method.safeArguments()?.size ?: 0)
        )
    }

    private fun rankingForAccessor(accessor: KtPropertyAccessor, method: Method): Ranking {
        val methodName = method.name()
        val expectedPropertyName = accessor.property.name ?: return ZERO

        if (accessor.isSetter) {
            if (!methodName.startsWith("set") || method.returnType() !is VoidType || method.argumentTypes().size != 1)
                return -MAJOR
        }

        if (accessor.isGetter) {
            if (!methodName.startsWith("get") && !methodName.startsWith("is"))
                return -MAJOR
            else if (method.returnType() is VoidType || method.argumentTypes().isNotEmpty())
                return -NORMAL
        }

        val actualPropertyName = getPropertyName(methodName, accessor.isSetter)
        return if (expectedPropertyName == actualPropertyName) NORMAL else -NORMAL
    }

    private fun getPropertyName(accessorMethodName: String, isSetter: Boolean): String {
        if (isSetter) {
            return accessorMethodName.drop(3)
        }

        return accessorMethodName.drop(if (accessorMethodName.startsWith("is")) 2 else 3)
    }

    private fun rankingForProperty(property: KtProperty, method: Method): Ranking {
        val methodName = method.name()
        val propertyName = property.name ?: return ZERO

        if (property.isTopLevel && method.name() == "<clinit>") {
            // For top-level property initializers
            return MINOR
        }

        if (!methodName.startsWith("get") && !methodName.startsWith("set"))
            return -MAJOR

        // boolean is
        return if (methodName.drop(3) == propertyName.capitalize()) MAJOR else -NORMAL
    }

    private fun rankingForVisibility(descriptor: DeclarationDescriptorWithVisibility, accessible: Accessible): Ranking {
        return collect(
            accessible.isPublic && descriptor.visibility == Visibilities.PUBLIC,
            accessible.isProtected && descriptor.visibility == Visibilities.PROTECTED,
            accessible.isPrivate && descriptor.visibility == Visibilities.PRIVATE
        )
    }

    private fun fileRankingSafe(file: KtFile, location: Location): Ranking {
        return try {
            fileRanking(file, location)
        } catch (e: ClassNotLoadedException) {
            LOG.error("ClassNotLoadedException should never happen in FileRankingCalculator", e)
            ZERO
        } catch (e: AbsentInformationException) {
            ZERO
        } catch (e: InternalException) {
            ZERO
        }
    }

    private fun fileRanking(file: KtFile, location: Location): Ranking {
        val locationLineNumber = location.lineNumber() - 1
        val lineStartOffset = file.getLineStartOffset(locationLineNumber) ?: return LOW
        val elementAt = file.findElementAt(lineStartOffset) ?: return ZERO

        var overallRanking = ZERO
        val method = location.method()

        if (method.isLambda()) {
            val (className, methodName) = method.getContainingClassAndMethodNameForLambda() ?: return ZERO
            if (method.isBridge && method.isSynthetic) {
                // It might be a static lambda field accessor
                val containingClass = elementAt.getParentOfType<KtClassOrObject>(false) ?: return LOW
                return rankingForClass(containingClass, className, location.virtualMachine())
            } else {
                val containingFunctionLiteral = findFunctionLiteralOnLine(elementAt) ?: return LOW

                val containingCallable = findNonLocalCallableParent(containingFunctionLiteral) ?: return LOW
                when (containingCallable) {
                    is KtFunction -> if (containingCallable.name == methodName) overallRanking += MAJOR
                    is KtProperty -> if (containingCallable.name == methodName) overallRanking += MAJOR
                    is KtPropertyAccessor -> if (containingCallable.property.name == methodName) overallRanking += MAJOR
                }

                val containingClass = containingCallable.getParentOfType<KtClassOrObject>(false)
                if (containingClass != null) {
                    overallRanking += rankingForClass(containingClass, className, location.virtualMachine())
                }

                return overallRanking
            }
        }

        // TODO support <clinit>
        if (method.name() == "<init>") {
            val containingClass = elementAt.getParentOfType<KtClassOrObject>(false) ?: return LOW
            val constructorOrInitializer =
                elementAt.getParentOfTypes2<KtConstructor<*>, KtClassInitializer>()?.takeIf { containingClass.isAncestor(it) }
                ?: containingClass.primaryConstructor?.takeIf { it.getLine() == containingClass.getLine() }

            if (constructorOrInitializer == null
                && locationLineNumber < containingClass.getLine()
                && locationLineNumber > containingClass.lastChild.getLine()
            ) {
                return LOW
            }

            overallRanking += rankingForClass(containingClass, location.declaringType())

            if (constructorOrInitializer is KtConstructor<*>)
                overallRanking += rankingForMethod(constructorOrInitializer, method)
        } else {
            val callable = findNonLocalCallableParent(elementAt) ?: return LOW
            overallRanking += when (callable) {
                is KtFunction -> rankingForMethod(callable, method)
                is KtPropertyAccessor -> rankingForAccessor(callable, method)
                is KtProperty -> rankingForProperty(callable, method)
                else -> return LOW
            }

            val containingClass = elementAt.getParentOfType<KtClassOrObject>(false)
            if (containingClass != null)
                overallRanking += rankingForClass(containingClass, location.declaringType())
        }

        return overallRanking
    }

    private fun findFunctionLiteralOnLine(element: PsiElement): KtFunctionLiteral? {
        val literal = element.getParentOfType<KtFunctionLiteral>(false)
        if (literal != null) {
            return literal
        }

        val callExpression = element.getParentOfType<KtCallExpression>(false) ?: return null

        for (lambdaArgument in callExpression.lambdaArguments) {
            if (element.getLine() == lambdaArgument.getLine()) {
                val functionLiteral = lambdaArgument.getLambdaExpression()?.functionLiteral
                if (functionLiteral != null) {
                    return functionLiteral
                }
            }
        }

        return null
    }

    private tailrec fun findNonLocalCallableParent(element: PsiElement): PsiElement? {
        fun PsiElement.isCallableDeclaration() = this is KtProperty || this is KtFunction || this is KtAnonymousInitializer

        // org.jetbrains.kotlin.psi.KtPsiUtil.isLocal
        fun PsiElement.isLocalDeclaration(): Boolean {
            val containingDeclaration = getParentOfType<KtDeclaration>(true)
            return containingDeclaration is KtCallableDeclaration || containingDeclaration is KtPropertyAccessor
        }

        if (element.isCallableDeclaration() && !element.isLocalDeclaration()) {
            return element
        }

        val containingCallable = element.getParentOfTypes3<KtProperty, KtFunction, KtAnonymousInitializer>()
            ?: return null

        if (containingCallable.isLocalDeclaration()) {
            return findNonLocalCallableParent(containingCallable)
        }

        return containingCallable
    }

    private fun Method.getContainingClassAndMethodNameForLambda(): Pair<String, String>? {
        // TODO this breaks nested classes
        val declaringClass = declaringType() as ClassType
        val (className, methodName) = declaringClass.name().split('$', limit = 3)
            .takeIf { it.size == 3 }
            ?: return null

        return Pair(className, methodName)
    }

    private fun Method.isLambda(): Boolean {
        val declaringClass = declaringType() as? ClassType ?: return false

        tailrec fun ClassType.isLambdaClass(): Boolean {
            if (interfaces().any { it.name() == FunctionBase::class.java.name }) {
                return true
            }

            val superClass = superclass() ?: return false
            return superClass.isLambdaClass()
        }

        return declaringClass.superclass().isLambdaClass()
    }

    private fun makeTypeMapper(bindingContext: BindingContext): KotlinTypeMapper {
        return KotlinTypeMapper(
            bindingContext, ClassBuilderMode.LIGHT_CLASSES, IncompatibleClassTracker.DoNothing, "debugger", JvmTarget.DEFAULT,
            KotlinTypeMapper.RELEASE_COROUTINES_DEFAULT, false
        )
    }

    companion object {
        val LOG = Logger.getInstance("FileRankingCalculator")
    }
}

private fun String.simpleName() = substringAfterLast('.').substringAfterLast('$')

private fun PsiElement.getLine(): Int {
    return DiagnosticUtils.getLineAndColumnInPsiFile(containingFile, textRange).line
}
