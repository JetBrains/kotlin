/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.model

import org.jetbrains.kotlin.codegen.ClassFileFactory

object BinaryArtifacts {
    class Jvm(val classFileFactory: ClassFileFactory) : ResultingArtifact.Binary<Jvm>() {
        override val kind: BinaryKind<Jvm>
            get() = ArtifactKinds.Jvm
    }

    class Js : ResultingArtifact.Binary<Js>() {
        override val kind: BinaryKind<Js>
            get() = ArtifactKinds.Js
    }

    class Native : ResultingArtifact.Binary<Native>() {
        override val kind: BinaryKind<Native>
            get() = ArtifactKinds.Native
    }

    class KLib : ResultingArtifact.Binary<KLib>() {
        override val kind: BinaryKind<KLib>
            get() = ArtifactKinds.KLib
    }
}
