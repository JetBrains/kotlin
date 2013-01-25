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

fun box() : String {
    StandardPipelineFactory({ print("OK") }).getPipeline()
    return "OK"
}
