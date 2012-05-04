package kt606

//KT-606 wrong resolved call

class StandardPipelineFactory(val config : ChannelPipeline.() -> Unit) : ChannelPipelineFactory {
    override fun getPipeline() : ChannelPipeline {
        val pipeline : ChannelPipeline = DefaultChannelPipeline()
        pipeline.config()
        return pipeline
    }
}

trait ChannelPipeline  {
  fun print(any: Any)
}

class DefaultChannelPipeline : ChannelPipeline {
  override fun print(any: Any) = System.out?.println(any)

}

trait ChannelPipelineFactory {
    fun getPipeline() : ChannelPipeline
}

fun testKt606() {
    StandardPipelineFactory({ print("OK") }).getPipeline()
}

//Tests for duplicates
//KT-1061 Can't call function defined as a val
object X {
    val doit = { (i: Int) -> i }
}

fun testKt1061() : String = if (X.doit(3) == 3) "OK" else "fail"


//KT-1249 IllegalStateException invoking function property
class TestClass(val body : () -> Unit) : Any {
    fun run() {
        body()
    }
}

fun testKt1249() {
    TestClass({}).run()
}

class Foo<T>(val filter: (T) -> Boolean) {
    public fun bar(tee: T) : Boolean {
        return filter(tee);
    }
}

//KT-1290 Method property in constructor causes NPE
fun testKt1290() = Foo({ (i: Int) -> i < 5 }).bar(2)

fun box() : String {
    testKt606()
    testKt1061()
    testKt1249()
    if (!testKt1290()) return "fail"
    return "OK"
}
