// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

interface SupervisorApiCallContextImpl : SupervisorApiCallDbContext, TxExecutor

interface SupervisorApiCallDbContext : TxExecutor, DbContextOwner

interface DbContextOwner : TxExecutor {
    override fun foo(p: Int) {}
}

interface TxExecutor {
    fun foo(p: Int = 1)
}