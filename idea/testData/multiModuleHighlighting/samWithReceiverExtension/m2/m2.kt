package m2

public fun testUseAsReceiver(api: javaInterface.API) {
    api.useM1A<String> {
        <error>this</error>.length
    }
    api.useM1B<String> {
        this.length
    }
    api.useM2A<String> {
        <error>this</error>.length
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
        <error>it</error>.length
    }
    api.useM2A<String> {
        it.length
    }
    api.useM2B<String> {
        <error>it</error>.length
    }
}