// TARGET_BACKEND: JVM_IR

abstract class AsyncJob {
    abstract suspend fun execute(lifetime: AsyncLifetime, attempt: Int, due: DateTime, context: JobContext): JobContext
}

class OrgBootstrapRequest
class AsyncLifetime
class DateTime
class JobContext

class OrgBootstrapTriggerJob(val orgId: Long, val bootstrap: OrgBootstrapRequest, val jetSalesSync: Boolean?) : AsyncJob() {
    override suspend fun execute(lifetime: AsyncLifetime, attempt: Int, due: DateTime, context: JobContext): JobContext {
        return JobContext()
    }
}

val name = "${OrgBootstrapTriggerJob::class.simpleName}.${OrgBootstrapTriggerJob::execute.name}"

fun box(): String {
    return "OK"
}