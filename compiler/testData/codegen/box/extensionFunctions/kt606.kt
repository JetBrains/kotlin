// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

package kt606

//KT-606 wrong resolved call

class StandardPipelineFactory(val config : ChannelPipeline.() -> Unit) : ChannelPipelineFactory {
    override fun getPipeline() : ChannelPipeline {
        val pipeline : ChannelPipeline = DefaultChannelPipeline()
        pipeline.config()
        return pipeline
    }
}

interface ChannelPipeline  {
  fun print(any: Any)
}

class DefaultChannelPipeline : ChannelPipeline {
  override fun print(any: Any) {
      System.out?.println(any)
  }

}

interface ChannelPipelineFactory {
    fun getPipeline() : ChannelPipeline
}

fun box() : String {
    StandardPipelineFactory({ print("OK") }).getPipeline()
    return "OK"
}
