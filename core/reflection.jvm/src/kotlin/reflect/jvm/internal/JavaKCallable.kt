/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Member
import java.lang.reflect.Modifier
import kotlin.reflect.KVisibility

internal abstract class JavaKCallable<out R>(
    override val container: KDeclarationContainerImpl,
    val member: Member,
    override val rawBoundReceiver: Any?,
) : ReflectKCallableImpl<R>() {
    override val visibility: KVisibility?
        get() = member.modifiers.computeVisibilityForJavaModifiers()

    final override val isFinal: Boolean
        get() = Modifier.isFinal(member.modifiers)

    final override val isOpen: Boolean
        get() = !isFinal && !isAbstract

    final override val isAbstract: Boolean
        get() = Modifier.isAbstract(member.modifiers)

    final override val isSuspend: Boolean
        get() = false
}
