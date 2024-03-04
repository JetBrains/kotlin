// FIR_IDENTICAL
// FILE: J.java
package test;

import org.jetbrains.annotations.Nullable;

class Editor<BC extends BuildConfiguration, TARGET extends BuildTarget<BC>> {
    public void onTargetSelected(@Nullable TARGET target) {}
}

interface BuildConfiguration {}
interface BuildTarget<BC extends BuildConfiguration> {}

class Helper extends AbstractHelper {}

abstract class AbstractHelper<BC extends BuildConfiguration, TARGET extends BuildTarget<BC>> {
    @Nullable
    public TARGET findRunTarget() {
        return null;
    }
}

// FILE: test.kt
package test

private fun test(
    editor: Editor<BuildConfiguration, BuildTarget<BuildConfiguration>>,
    helper: Helper,
) {
    editor.onTargetSelected(helper.findRunTarget())
}