// ISSUE: KT-69794

import Outer.Base

internal class Outer {
    interface Base
}

class Container {
    interface Derived : <!EXPOSED_SUPER_INTERFACE("public; Base; internal")!>Base<!>
}
