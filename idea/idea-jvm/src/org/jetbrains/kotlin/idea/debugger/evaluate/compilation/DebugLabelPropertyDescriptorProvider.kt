/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate.compilation

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.xdebugger.impl.XDebugSessionImpl
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup
import org.jetbrains.kotlin.builtins.PrimitiveType
import com.sun.jdi.*
import com.sun.jdi.Type as JdiType
import org.jetbrains.kotlin.backend.common.lower.SimpleMemberScope
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.idea.debugger.evaluate.getClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.externalDescriptors
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.Type as AsmType

class DebugLabelPropertyDescriptorProvider(val codeFragment: KtCodeFragment, val debugProcess: DebugProcessImpl) {
    companion object {
        fun getMarkupMap(debugProcess: DebugProcessImpl) = doGetMarkupMap(debugProcess) ?: emptyMap()

        private fun doGetMarkupMap(debugProcess: DebugProcessImpl): Map<out Value?, ValueMarkup>? {
            if (ApplicationManager.getApplication().isUnitTestMode) {
                return NodeDescriptorImpl.getMarkupMap(debugProcess)
            }

            val debugSession = debugProcess.session.xDebugSession as? XDebugSessionImpl

            @Suppress("UNCHECKED_CAST")
            return debugSession?.valueMarkers?.allMarkers?.filterKeys { it is Value? } as Map<out Value?, ValueMarkup>?
        }
    }

    private val moduleDescriptor = DebugLabelModuleDescriptor

    fun supplyDebugLabels() {
        val packageFragment = object : PackageFragmentDescriptorImpl(moduleDescriptor, FqName.ROOT) {
            val properties = createDebugLabelDescriptors(this)
            override fun getMemberScope() = SimpleMemberScope(properties)
        }

        codeFragment.externalDescriptors = packageFragment.properties
    }

    private fun createDebugLabelDescriptors(containingDeclaration: PackageFragmentDescriptor): List<PropertyDescriptor> {
        val markupMap = getMarkupMap(debugProcess)

        val result = ArrayList<PropertyDescriptor>(markupMap.size)

        nextValue@ for ((value, markup) in markupMap) {
            val labelName = markup.text
            val kotlinType = value?.type()?.let { convertType(it) } ?: moduleDescriptor.builtIns.nullableAnyType
            result += createDebugLabelDescriptor(labelName, kotlinType, containingDeclaration)
        }

        return result
    }

    private fun createDebugLabelDescriptor(
        labelName: String,
        type: KotlinType,
        containingDeclaration: PackageFragmentDescriptor
    ): PropertyDescriptor {
        val propertyDescriptor = DebugLabelPropertyDescriptor(containingDeclaration, labelName)
        propertyDescriptor.setType(type, emptyList(), null, null)

        val getterDescriptor = PropertyGetterDescriptorImpl(
            propertyDescriptor,
            Annotations.EMPTY,
            Modality.FINAL,
            Visibilities.PUBLIC,
            /* isDefault = */ false,
            /* isExternal = */ false,
            /* isInline = */ false,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            /* original = */ null,
            SourceElement.NO_SOURCE
        ).apply { initialize(type) }

        propertyDescriptor.initialize(getterDescriptor, null)
        return propertyDescriptor
    }

    private fun convertType(type: JdiType): KotlinType {
        val builtIns = moduleDescriptor.builtIns

        return when (type) {
            is VoidType -> builtIns.unitType
            is LongType -> builtIns.longType
            is DoubleType -> builtIns.doubleType
            is CharType -> builtIns.charType
            is FloatType -> builtIns.floatType
            is ByteType -> builtIns.byteType
            is IntegerType -> builtIns.intType
            is BooleanType -> builtIns.booleanType
            is ShortType -> builtIns.shortType
            is ArrayType -> {
                when (val componentType = type.componentType()) {
                    is VoidType -> builtIns.getArrayType(Variance.INVARIANT, builtIns.unitType)
                    is LongType -> builtIns.getPrimitiveArrayKotlinType(PrimitiveType.LONG)
                    is DoubleType -> builtIns.getPrimitiveArrayKotlinType(PrimitiveType.DOUBLE)
                    is CharType -> builtIns.getPrimitiveArrayKotlinType(PrimitiveType.CHAR)
                    is FloatType -> builtIns.getPrimitiveArrayKotlinType(PrimitiveType.FLOAT)
                    is ByteType -> builtIns.getPrimitiveArrayKotlinType(PrimitiveType.BYTE)
                    is IntegerType -> builtIns.getPrimitiveArrayKotlinType(PrimitiveType.INT)
                    is BooleanType -> builtIns.getPrimitiveArrayKotlinType(PrimitiveType.BOOLEAN)
                    is ShortType -> builtIns.getPrimitiveArrayKotlinType(PrimitiveType.SHORT)
                    else -> builtIns.getArrayType(Variance.INVARIANT, convertReferenceType(componentType))
                }
            }
            is ReferenceType -> convertReferenceType(type)
            else -> builtIns.anyType
        }
    }

    private fun convertReferenceType(type: JdiType): KotlinType {
        require(type is ClassType || type is InterfaceType)

        val asmType = AsmType.getType(type.signature())
        val project = codeFragment.project
        val classDescriptor = asmType.getClassDescriptor(GlobalSearchScope.allScope(project), mapBuiltIns = false)
            ?: return moduleDescriptor.builtIns.nullableAnyType
        return classDescriptor.defaultType
    }
}

private object DebugLabelModuleDescriptor
    : DeclarationDescriptorImpl(Annotations.EMPTY, Name.identifier("DebugLabelExtensions")),
    ModuleDescriptor
{
    override val builtIns: KotlinBuiltIns
        get() = DefaultBuiltIns.Instance

    override val stableName: Name?
        get() = name

    override fun shouldSeeInternalsOf(targetModule: ModuleDescriptor) = false

    override fun getPackage(fqName: FqName): PackageViewDescriptor {
        return object : PackageViewDescriptor, DeclarationDescriptorImpl(Annotations.EMPTY, FqName.ROOT.shortNameOrSpecial()) {
            override fun getContainingDeclaration(): PackageViewDescriptor? = null

            override val fqName: FqName
                get() = FqName.ROOT

            override val memberScope: MemberScope
                get() = MemberScope.Empty

            override val module: ModuleDescriptor
                get() = this@DebugLabelModuleDescriptor

            override val fragments: List<PackageFragmentDescriptor>
                get() = emptyList()

            override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
                return visitor.visitPackageViewDescriptor(this, data)
            }
        }
    }

    override val platform: TargetPlatform?
        get() = DefaultBuiltInPlatforms.jvmPlatform

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        return emptyList()
    }

    override val allDependencyModules: List<ModuleDescriptor>
        get() = emptyList()

    override val expectedByModules: List<ModuleDescriptor>
        get() = emptyList()

    override fun <T> getCapability(capability: ModuleDescriptor.Capability<T>): T? = null

    override val isValid: Boolean
        get() = true

    override fun assertValid() {}
}

internal class DebugLabelPropertyDescriptor(
    containingDeclaration: DeclarationDescriptor,
    val labelName: String
) : PropertyDescriptorImpl(
    containingDeclaration,
    null,
    Annotations.EMPTY,
    Modality.FINAL,
    Visibilities.PUBLIC,
    /*isVar = */false,
    Name.identifier(labelName + "_DebugLabel"),
    CallableMemberDescriptor.Kind.SYNTHESIZED,
    SourceElement.NO_SOURCE,
    /*lateInit = */false,
    /*isConst = */false,
    /*isExpect = */false,
    /*isActual = */false,
    /*isExternal = */false,
    /*isDelegated = */false
)