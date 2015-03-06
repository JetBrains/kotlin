package a

import a.A.*
import a.A.C
import a.A.<!CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON!>C<!>.*
import a.A.<!CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON!>D<!>.*
import a.A.<!CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON!>C<!>.*
import a.A.C.G
import a.A.E.<!CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON!>J<!>.*
import a.A.<!CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON!>CO<!>.*
import a.A.CO

import a.B.C.*
import a.B.C.<!CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON!>A<!>.*
import a.B.C.<!CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON!>D<!>.*

import a.E.*
import a.E.E1
import a.E.<!CANNOT_IMPORT_ON_DEMAND_FROM_SINGLETON!>E2<!>.*

class A {
    object C {
        object G
    }
    object D {

    }

    class E {
        object J
    }

    default object CO {
        object H
    }
}

enum class E {
    E1 E2
}

object B {
    class C {
        object A
        object D
    }
}