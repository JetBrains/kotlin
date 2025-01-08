// RUN_PIPELINE_TILL: BACKEND
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class MarkerA

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class MarkerB

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
annotation class MarkerC

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
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
    override fun <!OPT_IN_OVERRIDE("MarkerA; Base declaration of supertype 'OverrideOptInA' requires opt-in to be overridden. The overriding declaration should be annotated with '@MarkerA' or '@OptIn(MarkerA::class)'")!>overrideOptIn<!>(){}
}

class OverrideOptInImplB : OverrideOptInB {
    override fun <!OPT_IN_OVERRIDE("MarkerB; Base declaration of supertype 'OverrideOptInB' requires opt-in to be overridden. The overriding declaration should be annotated with '@MarkerB' or '@OptIn(MarkerB::class)'")!>overrideOptIn<!>(){}
}

class OverrideOptInImplC : OverrideOptInC {
    override fun <!OPT_IN_OVERRIDE("MarkerC; Base declaration of supertype 'OverrideOptInC' requires opt-in to be overridden. The overriding declaration should be annotated with '@MarkerC' or '@OptIn(MarkerC::class)'")!>overrideOptIn<!>(){}
}


class OverrideOptInImplD : OverrideOptInD {
    override fun <!OPT_IN_OVERRIDE("MarkerD; Base declaration of supertype 'OverrideOptInD' requires opt-in to be overridden. The overriding declaration should be annotated with '@MarkerD' or '@OptIn(MarkerD::class)'")!>overrideOptIn<!>(){}
}

