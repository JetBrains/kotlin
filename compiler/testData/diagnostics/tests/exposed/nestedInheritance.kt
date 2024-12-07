// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-69794
// RENDER_DIAGNOSTICS_FULL_TEXT

import Outer.Base

internal class Outer {
    interface Base
}

class Container {
    interface Derived : <!EXPOSED_SUPER_INTERFACE("public;  containing declaration Outer; internal")!>Base<!>
}
