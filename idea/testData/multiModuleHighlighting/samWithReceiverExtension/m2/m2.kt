package m2

public fun testUseAsReceiver(api: javaInterface.API) {
    api.useM1A<String> {
        <error descr="[NO_THIS] 'this' is not defined in this context">this</error>.<error descr="[DEBUG] Reference is not resolved to anything, but is not marked unresolved">length</error>
    }
    api.useM1B<String> {
        this.length
    }
    api.useM2A<String> {
        <error descr="[NO_THIS] 'this' is not defined in this context">this</error>.<error descr="[DEBUG] Reference is not resolved to anything, but is not marked unresolved">length</error>
    }
    api.useM2B<String> {
        this.length
    }
}

public fun testUseAsParameter(api: javaInterface.API) {
    api.useM1A<String> {
        it.length
    }
    api.useM1B<String> {
        <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: it">it</error>.<error descr="[DEBUG] Reference is not resolved to anything, but is not marked unresolved">length</error>
    }
    api.useM2A<String> {
        it.length
    }
    api.useM2B<String> {
        <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: it">it</error>.<error descr="[DEBUG] Reference is not resolved to anything, but is not marked unresolved">length</error>
    }
}