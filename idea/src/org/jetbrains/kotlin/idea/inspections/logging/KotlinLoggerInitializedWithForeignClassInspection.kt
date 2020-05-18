/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.logging

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.ListTable
import com.intellij.codeInspection.ui.ListWrappingTableModel
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.Accessor
import com.intellij.util.xmlb.SerializationFilterBase
import com.intellij.util.xmlb.XmlSerializer
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.ui.UiUtils
import org.jdom.Element
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import javax.swing.JComponent

class KotlinLoggerInitializedWithForeignClassInspection : AbstractKotlinInspection() {
    companion object {
        private val DEFAULT_LOGGER_FACTORIES = listOf(
            "java.util.logging.Logger" to "getLogger",
            "org.slf4j.LoggerFactory" to "getLogger",
            "org.apache.commons.logging.LogFactory" to "getLog",
            "org.apache.log4j.Logger" to "getLogger",
            "org.apache.logging.log4j.LogManager" to "getLogger",
        )
        private val DEFAULT_LOGGER_FACTORY_CLASS_NAMES = DEFAULT_LOGGER_FACTORIES.map { it.first }
        private val DEFAULT_LOGGER_FACTORY_METHOD_NAMES = DEFAULT_LOGGER_FACTORIES.map { it.second }
        private val DEFAULT_LOGGER_FACTORY_CLASS_NAME = BaseInspection.formatString(DEFAULT_LOGGER_FACTORY_CLASS_NAMES)
        private val DEFAULT_LOGGER_FACTORY_METHOD_NAME = BaseInspection.formatString(DEFAULT_LOGGER_FACTORY_METHOD_NAMES)
    }

    @Suppress("MemberVisibilityCanBePrivate")
    var loggerFactoryClassName: String = DEFAULT_LOGGER_FACTORY_CLASS_NAME

    @Suppress("MemberVisibilityCanBePrivate")
    var loggerFactoryMethodName: String = DEFAULT_LOGGER_FACTORY_METHOD_NAME

    private val loggerFactoryClassNames = DEFAULT_LOGGER_FACTORY_CLASS_NAMES.toMutableList()
    private val loggerFactoryMethodNames = DEFAULT_LOGGER_FACTORY_METHOD_NAMES.toMutableList()
    private val loggerFactoryFqNames
        get() = loggerFactoryClassNames.zip(loggerFactoryMethodNames).groupBy(
            { (_, methodName) -> methodName },
            { (className, methodName) -> FqName("${className}.${methodName}") }
        )

    override fun createOptionsPanel(): JComponent? {
        val table = ListTable(
            ListWrappingTableModel(
                listOf(loggerFactoryClassNames, loggerFactoryMethodNames),
                KotlinBundle.message("logger.factory.class.name"),
                KotlinBundle.message("logger.factory.method.name")
            )
        )
        return UiUtils.createAddRemoveTreeClassChooserPanel(table, KotlinBundle.message("choose.logger.factory.class"))
    }

    override fun readSettings(element: Element) {
        super.readSettings(element)
        BaseInspection.parseString(loggerFactoryClassName, loggerFactoryClassNames)
        BaseInspection.parseString(loggerFactoryMethodName, loggerFactoryMethodNames)
        if (loggerFactoryClassNames.isEmpty() || loggerFactoryClassNames.size != loggerFactoryMethodNames.size) {
            BaseInspection.parseString(DEFAULT_LOGGER_FACTORY_CLASS_NAME, loggerFactoryClassNames)
            BaseInspection.parseString(DEFAULT_LOGGER_FACTORY_METHOD_NAME, loggerFactoryMethodNames)
        }
    }

    override fun writeSettings(element: Element) {
        loggerFactoryClassName = BaseInspection.formatString(loggerFactoryClassNames)
        loggerFactoryMethodName = BaseInspection.formatString(loggerFactoryMethodNames)
        XmlSerializer.serializeInto(this, element, object : SerializationFilterBase() {
            override fun accepts(accessor: Accessor, bean: Any, beanValue: Any?): Boolean {
                if (accessor.name == "loggerFactoryClassName" && beanValue == DEFAULT_LOGGER_FACTORY_CLASS_NAME) return false
                if (accessor.name == "loggerFactoryMethodName" && beanValue == DEFAULT_LOGGER_FACTORY_METHOD_NAME) return false
                return true
            }
        })
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = callExpressionVisitor(fun(call) {
        val containingClassName = call.containingClass()?.name ?: return

        val callee = call.calleeExpression ?: return
        val loggerMethodFqNames = loggerFactoryFqNames[callee.text] ?: return

        val argument = call.valueArguments.singleOrNull()?.getArgumentExpression() as? KtDotQualifiedExpression ?: return
        val argumentSelector = argument.selectorExpression ?: return
        val classLiteral = when (val argumentReceiver = argument.receiverExpression) {
            // Foo::class.java, Foo::class.qualifiedName, Foo::class.simpleName
            is KtClassLiteralExpression -> {
                val selectorText = argumentSelector.safeAs<KtNameReferenceExpression>()?.text
                if (selectorText !in listOf("java", "qualifiedName", "simpleName")) return
                argumentReceiver
            }
            // Foo::class.java.name, Foo::class.java.simpleName, Foo::class.java.canonicalName
            is KtDotQualifiedExpression -> {
                val classLiteral = argumentReceiver.receiverExpression as? KtClassLiteralExpression ?: return
                if (argumentReceiver.selectorExpression.safeAs<KtNameReferenceExpression>()?.text != "java") return
                val selectorText = argumentSelector.safeAs<KtNameReferenceExpression>()?.text
                    ?: argumentSelector.safeAs<KtCallExpression>()?.calleeExpression?.text
                if (selectorText !in listOf("name", "simpleName", "canonicalName", "getName", "getSimpleName", "getCanonicalName")) return
                classLiteral
            }
            else -> return
        }
        val classLiteralName = classLiteral.receiverExpression?.text ?: return
        if (containingClassName == classLiteralName) return

        if (call.resolveToCall()?.resultingDescriptor?.fqNameOrNull() !in loggerMethodFqNames) return

        holder.registerProblem(
            classLiteral,
            KotlinBundle.message("logger.initialized.with.foreign.class", "$classLiteralName::class"),
            ReplaceForeignFix(containingClassName)
        )
    })

    private class ReplaceForeignFix(private val containingClassName: String) : LocalQuickFix {
        override fun getName() = KotlinBundle.message("replace.with.0", "$containingClassName::class")

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val argument = descriptor.psiElement.getStrictParentOfType<KtValueArgument>()?.getArgumentExpression()
                    as? KtDotQualifiedExpression ?: return
            val receiver = argument.receiverExpression
            val selector = argument.selectorExpression ?: return
            val psiFactory = KtPsiFactory(argument)
            val newArgument = when (receiver) {
                is KtClassLiteralExpression -> {
                    psiFactory.createExpressionByPattern("${containingClassName}::class.$0", selector)
                }
                is KtDotQualifiedExpression -> {
                    psiFactory.createExpressionByPattern("${containingClassName}::class.java.$0", selector)
                }
                else -> return
            }
            argument.replace(newArgument)
        }
    }
}
