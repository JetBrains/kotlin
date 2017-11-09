package m1

public fun testUseAsReceiver(api: javaInterface.API) {
    api.useM1A<String> {
        this.length
    }
    api.useM1B<String> {
        <error>this</error>.length
    }
    api.useM2A<String> {
        this.length
    }
    api.useM2B<String> {
        <error>this</error>.length
    }
}

public fun testUseAsParameter(api: javaInterface.API) {
    api.useM1A<String> {
        <error>it</error>.length
    }
    api.useM1B<String> {
        it.length
    }
    api.useM2A<String> {
        <error>it</error>.length
    }
    api.useM2B<String> {
        it.length
    }
}