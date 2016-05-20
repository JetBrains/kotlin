// "Make 'Derived' private" "true"
// ACTION: Make 'Derived' internal

import Outer.Base

internal class Outer {
    interface Base
}

class Container {
    interface Derived : <caret>Base
}