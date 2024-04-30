// FIR_IDENTICAL

// FILE: test/Editor.java
package test;

import org.jetbrains.annotations.Nullable;

class Editor<BC extends BuildConfiguration, TARGET extends BuildTarget<BC>> {
    public void onTargetSelected(@Nullable TARGET target) {}
}

// FILE: test/BuildConfiguration.java
package test;

interface BuildConfiguration {}

// FILE: test/BuildTarget.java
package test;

interface BuildTarget<BC extends BuildConfiguration> {}

// FILE: test/Helper.java
package test;

class Helper extends AbstractHelper {}

// FILE: test/AbstractHelper.java
package test;

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