/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

object JavaVisibilities {
    object PackageVisibility : Visibility("package", isPublicAPI = false) {
        override fun compareTo(visibility: Visibility): Int? {
            if (this === visibility) return 0
            if (Visibilities.isPrivate(visibility)) return 1
            return -1
        }

        override fun normalize(): Visibility {
            return Visibilities.Protected
        }

        override val internalDisplayName: String
            get() = "public/*package*/"
    }

    object ProtectedStaticVisibility : Visibility("protected_static", isPublicAPI = true) {
        override fun normalize(): Visibility {
            return Visibilities.Protected
        }

        override val internalDisplayName: String
            get() = "protected/*protected static*/"
    }

    object ProtectedAndPackage : Visibility("protected_and_package", isPublicAPI = true) {
        override fun compareTo(visibility: Visibility): Int? {
            if (this == visibility) return 0
            if (visibility === Visibilities.Internal) return null
            return if (Visibilities.isPrivate(visibility)) 1 else -1
        }

        override fun normalize(): Visibility {
            return Visibilities.Protected
        }

        override val internalDisplayName: String
            get() = "protected/*protected and package*/"
    }
}
