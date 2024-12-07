package org.jetbrains.kotlin.backend.common.overrides

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrOverridableMember
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

class FakeOverrideChecker(
    private val irMangler: KotlinMangler.IrMangler,
    private val descriptorMangler: KotlinMangler.DescriptorMangler
) {

    private fun checkOverriddenSymbols(fake: IrOverridableMember) {
        if (fake !is IrSimpleFunction) return // TODO: we need overridden symbols on IrProperty.
        fake.overriddenSymbols.forEach { symbol ->
            assert((symbol.owner.parent as IrClass).declarations.contains(symbol.owner)) {
                "CHECK overridden symbols: ${fake.render()} refers to ${symbol.owner.render()} which is not a member of ${symbol.owner.parent.render()}"
            }
        }
    }

    private fun validateFakeOverrides(clazz: IrClass, compatibleMode: Boolean = false) {
        val classId = clazz.classId ?: return
        val classDescriptor = clazz.module.module.findClassAcrossModuleDependencies(classId) ?: return
        // All enum entry overrides look like fake overrides in descriptor enum entries
        if (classDescriptor.kind == ClassKind.ENUM_ENTRY) return

        val descriptorFakeOverrides = classDescriptor.unsubstitutedMemberScope
            .getDescriptorsFiltered(DescriptorKindFilter.CALLABLES)
            .asSequence()
            .filterIsInstance<CallableMemberDescriptor>()
            .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
            .filterNot { it.visibility == DescriptorVisibilities.PRIVATE || it.visibility == DescriptorVisibilities.INVISIBLE_FAKE }

        val descriptorSignatures = descriptorFakeOverrides
            .map { with(descriptorMangler) { it.signatureString(compatibleMode) } }
            .toMutableList()
            .apply { sort() }

        val irFakeOverrides = clazz.declarations.asSequence()
            .filterIsInstance<IrOverridableMember>()
            .filter { it.isFakeOverride }
            .onEach { checkOverriddenSymbols(it) }

        val irSignatures = irFakeOverrides
            .map { with(irMangler) { it.signatureString(compatibleMode) } }
            .toMutableList()
            .apply { sort() }

        // We can't have equality here because dependency libraries could have
        // been compiled with -friend-modules.
        // There can be internal fake overrides in IR absent in descriptors.
        // Also there can be members inherited from internal interfaces of other modules.
        require(irSignatures.containsAll(descriptorSignatures)) {
            "[IR VALIDATION] Internal fake override mismatch for ${clazz.fqNameWhenAvailable!!}\n" +
            "\tDescriptor based: $descriptorSignatures\n" +
            "\tIR based        : $irSignatures"
        }
    }

    fun check(module: IrModuleFragment) {
        module.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }
            override fun visitClass(declaration: IrClass) {
                validateFakeOverrides(declaration)
                super.visitClass(declaration)
            }
        })
    }
}
