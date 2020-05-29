package org.jetbrains.kotlin.backend.common.overrides

import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrOverridableMember
import org.jetbrains.kotlin.ir.util.module
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

// The contents of this file is from VisibilityUtil.kt adapted to IR.
// TODO: The code would better be commonized for descriptors, ir and fir.

private fun findInvisibleMember(
    receiver: ReceiverValue?,
    what: IrDeclarationWithVisibility,
    from: IrDeclaration
): IrDeclarationWithVisibility? {
    // TODO: We need the proper code for IR,
    // but that requires many of Visibilities.java available in IR.
    // So for now we stick to a quick hack serving the needs
    // of fake override construction algorithm.
    val parentsWithSelf = sequenceOf(what) + what.parents
    parentsWithSelf.forEach {
        if (it !is IrDeclarationWithVisibility) return null
        if (it.visibility == Visibilities.INTERNAL &&
            (from.module != it.module)
        ) {
            return it
        }
    }
    return null
}

fun isVisibleIgnoringReceiver(
    what: IrDeclarationWithVisibility,
    from: IrDeclaration
): Boolean {
    return findInvisibleMember(Visibilities.ALWAYS_SUITABLE_RECEIVER, what, from) == null
}

fun isVisibleForOverride(
    overriding: IrOverridableMember,
    fromSuper: IrOverridableMember
): Boolean {
    return !Visibilities.isPrivate((fromSuper as IrDeclarationWithVisibility).visibility) &&
            isVisibleIgnoringReceiver(fromSuper, overriding)
}

fun findMemberWithMaxVisibility(members: Collection<IrOverridableMember>): IrOverridableMember {
    assert(members.isNotEmpty())

    var member: IrOverridableMember? = null
    for (candidate in members) {
        if (member == null) {
            member = candidate
            continue
        }

        val result = Visibilities.compare(member.visibility, candidate.visibility)
        if (result != null && result < 0) {
            member = candidate
        }
    }
    return member ?: error("Could not find a visible member")
}
