// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74421

open class OpenBase {
    val x: String = "ok"
}

class InitOpenViaSuper : OpenBase() {
    init {
        <!VAL_REASSIGNMENT!>super.x<!> = "error"
    }
}

class InitOpenViaThis : OpenBase() {
    init {
        <!VAL_REASSIGNMENT!>this.x<!> = "error"
    }
}

class InitOpenViaImplicit : OpenBase() {
    init {
        <!VAL_REASSIGNMENT!>x<!> = "error"
    }
}

open class GenericOpenBase<T> {
    val x: String = "ok"
}

class InitGenericOpenViaSuper : GenericOpenBase<String>() {
    init {
        <!VAL_REASSIGNMENT!>super.x<!> = "error"
    }
}

class InitGenericOpenViaThis : GenericOpenBase<String>() {
    init {
        <!VAL_REASSIGNMENT!>this.x<!> = "error"
    }
}

class InitGenericOpenViaImplicit : GenericOpenBase<String>() {
    init {
        <!VAL_REASSIGNMENT!>x<!> = "error"
    }
}

interface InterfaceBase {
    val x: String
}

class InitOpenAndInterfaceViaSuper : OpenBase(), InterfaceBase {
    init {
        <!VAL_REASSIGNMENT!>super.x<!> = "error"
    }
}

class InitOpenAndInterfaceViaThis : OpenBase(), InterfaceBase {
    init {
        <!VAL_REASSIGNMENT!>this.x<!> = "error"
    }
}

class InitOpenAndInterfaceViaImplicit : OpenBase(), InterfaceBase {
    init {
        <!VAL_REASSIGNMENT!>x<!> = "error"
    }
}
