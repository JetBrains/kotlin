package org.jetbrains.kotlin.backend.common.overrides

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered

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

    private fun validateFakeOverrides(clazz: IrClass) {
        val classId = clazz.classId ?: return
        val classDescriptor = clazz.module.module.findClassAcrossModuleDependencies(classId) ?: return
        // All enum entry overrides look like fake overrides in descriptor enum entries
        if (classDescriptor.kind == ClassKind.ENUM_ENTRY) return

        val descriptorFakeOverrides = classDescriptor.unsubstitutedMemberScope
            .getDescriptorsFiltered(DescriptorKindFilter.CALLABLES)
            .filterIsInstance<CallableMemberDescriptor>()
            .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
            .filterNot { it.visibility == Visibilities.PRIVATE || it.visibility == Visibilities.INVISIBLE_FAKE }

        val descriptorSignatures = descriptorFakeOverrides
            .map { with(descriptorMangler) { it.signatureString }}
            .sorted()

        val irFakeOverrides = clazz.declarations
            .filterIsInstance<IrOverridableMember>()
            .filter { it.isFakeOverride }

        irFakeOverrides.forEach {
            checkOverriddenSymbols(it)
        }

        val irSignatures = irFakeOverrides
            .map { with(irMangler) { it.signatureString }}
            .sorted()

        assert(descriptorSignatures == irSignatures) {
            "[IR VALIDATION] Fake override mismatch for ${clazz.fqNameWhenAvailable!!}\n" +
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
            override fun visitFunction(declaration: IrFunction) {
                // Don't go for function local classes
            }
        })
    }
}
