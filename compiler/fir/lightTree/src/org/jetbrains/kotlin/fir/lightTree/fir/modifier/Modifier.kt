/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.impl.FirAbstractAnnotatedElement

class Modifier(
    session: FirSession,
    psi: PsiElement? = null,

    var classModifier: ClassModifier? = null,
    var memberModifier: MemberModifier? = null,
    var visibilityModifier: VisibilityModifier = VisibilityModifier.UNKNOWN,
    var functionModifier: FunctionModifier? = null,
    var propertyModifier: PropertyModifier? = null,
    var inheritanceModifier: InheritanceModifier? = null,
    var parameterModifier: ParameterModifier? = null,
    var platformModifier: PlatformModifier? = null
) : FirAbstractAnnotatedElement(session, psi)

enum class ClassModifier{
    ENUM,
    ANNOTATION,
    DATA,
    INNER,
    COMPANION
}

enum class MemberModifier{
    OVERRIDE,
    LATEINIT
}

enum class VisibilityModifier{
    PUBLIC{
        override fun toVisibility(): Visibility {
            return Visibilities.PUBLIC
        }
    },
    PRIVATE{
        override fun toVisibility(): Visibility {
            return Visibilities.PRIVATE
        }
    },
    INTERNAL{
        override fun toVisibility(): Visibility {
            return Visibilities.INTERNAL
        }
    },
    PROTECTED{
        override fun toVisibility(): Visibility {
            return Visibilities.PROTECTED
        }
    },
    UNKNOWN{
        override fun toVisibility(): Visibility {
            return Visibilities.UNKNOWN
        }
    };

    abstract fun toVisibility(): Visibility
}

enum class FunctionModifier{
    TAILREC,
    OPERATOR,
    INFIX,
    INLINE,
    EXTERNAL,
    SUSPEND
}

enum class PropertyModifier{
    CONST
}

enum class InheritanceModifier{
    ABSTRACT{
        override fun toModality(): Modality {
            return Modality.ABSTRACT
        }
    },
    FINAL{
        override fun toModality(): Modality {
            return Modality.FINAL
        }
    },
    OPEN{
        override fun toModality(): Modality {
            return Modality.OPEN
        }
    },
    SEALED{
        override fun toModality(): Modality {
            return Modality.SEALED
        }
    };

    abstract fun toModality(): Modality
}

enum class ParameterModifier {
    VARARG,
    NOINLINE,
    CROSSINLINE
}

enum class PlatformModifier{
    EXPECT,
    ACTUAL
}
