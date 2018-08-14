package org.jetbrains.konan.analyser.index;

import org.jetbrains.kotlin.idea.util.KotlinBinaryExtension;

public class KonanMetaBinary extends KotlinBinaryExtension {
    public KonanMetaBinary() {
        super(KonanMetaFileType.INSTANCE);
    }
}
