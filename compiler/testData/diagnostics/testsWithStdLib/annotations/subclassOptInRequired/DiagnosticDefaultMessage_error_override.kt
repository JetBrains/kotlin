// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
@RequiresOptIn()
annotation class MarkerA

@RequiresOptIn()
annotation class MarkerB

@RequiresOptIn()
annotation class MarkerC

@RequiresOptIn()
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
    override fun <!OPT_IN_OVERRIDE_ERROR("MarkerA; Base declaration of supertype 'OverrideOptInA' needs opt-in. The declaration override must be annotated with '@MarkerA' or '@OptIn(MarkerA::class)'")!>overrideOptIn<!>(){}
}

class OverrideOptInImplB : OverrideOptInB {
    override fun <!OPT_IN_OVERRIDE_ERROR("MarkerB; Base declaration of supertype 'OverrideOptInB' needs opt-in. The declaration override must be annotated with '@MarkerB' or '@OptIn(MarkerB::class)'")!>overrideOptIn<!>(){}
}

class OverrideOptInImplC : OverrideOptInC {
    override fun <!OPT_IN_OVERRIDE_ERROR("MarkerC; Base declaration of supertype 'OverrideOptInC' needs opt-in. The declaration override must be annotated with '@MarkerC' or '@OptIn(MarkerC::class)'")!>overrideOptIn<!>(){}
}


class OverrideOptInImplD : OverrideOptInD {
    override fun <!OPT_IN_OVERRIDE_ERROR("MarkerD; Base declaration of supertype 'OverrideOptInD' needs opt-in. The declaration override must be annotated with '@MarkerD' or '@OptIn(MarkerD::class)'")!>overrideOptIn<!>(){}
}

