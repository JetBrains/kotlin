// RUN_PIPELINE_TILL: FRONTEND
@RequiresOptIn("please do something about this.")
annotation class MarkerA

@RequiresOptIn("please do something about this")
annotation class MarkerB

@RequiresOptIn("please do something about this!")
annotation class MarkerC

@RequiresOptIn("please do something about this?")
annotation class MarkerD

interface OverrideOptInA {
    @MarkerA
    fun overrideOptIn() {
    }
}

interface OverrideOptInB {
    @MarkerB
    fun overrideOptIn() {
    }
}

interface OverrideOptInC {
    @MarkerC
    fun overrideOptIn() {
    }
}


interface OverrideOptInD {
    @MarkerD
    fun overrideOptIn() {
    }
}


class OverrideOptInImplA : OverrideOptInA {
    override fun <!OPT_IN_OVERRIDE_ERROR("MarkerA; Base declaration of supertype 'OverrideOptInA' requires opt-in to be overridden: please do something about this. The overriding declaration must be annotated with '@MarkerA' or '@OptIn(MarkerA::class)'")!>overrideOptIn<!>(){}
}

class OverrideOptInImplB : OverrideOptInB {
    override fun <!OPT_IN_OVERRIDE_ERROR("MarkerB; Base declaration of supertype 'OverrideOptInB' requires opt-in to be overridden: please do something about this. The overriding declaration must be annotated with '@MarkerB' or '@OptIn(MarkerB::class)'")!>overrideOptIn<!>(){}
}

class OverrideOptInImplC : OverrideOptInC {
    override fun <!OPT_IN_OVERRIDE_ERROR("MarkerC; Base declaration of supertype 'OverrideOptInC' requires opt-in to be overridden: please do something about this! The overriding declaration must be annotated with '@MarkerC' or '@OptIn(MarkerC::class)'")!>overrideOptIn<!>(){}
}


class OverrideOptInImplD : OverrideOptInD {
    override fun <!OPT_IN_OVERRIDE_ERROR("MarkerD; Base declaration of supertype 'OverrideOptInD' requires opt-in to be overridden: please do something about this? The overriding declaration must be annotated with '@MarkerD' or '@OptIn(MarkerD::class)'")!>overrideOptIn<!>(){}
}

