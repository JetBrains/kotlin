class A {
    inline suspend fun <reified T> publicFun() {}
    internal inline suspend fun <reified T> internalFun() {}
    protected inline suspend fun <reified T> protectedFun() {}
    private inline suspend fun <reified T> privateFun() {}
}

inline suspend fun <reified T> publicFun() {}
internal inline suspend fun <reified T> internalFun() {}
private inline suspend fun <reified T> privateFun() {}
