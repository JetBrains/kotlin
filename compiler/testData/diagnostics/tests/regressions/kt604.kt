// KT-604 Internal frontend error

trait ChannelPipeline {

}

class DefaultChannelPipeline() : ChannelPipeline {
}

trait ChannelPipelineFactory{
    fun getPipeline() : ChannelPipeline
}

class StandardPipelineFactory(val config:  ChannelPipeline.()->Unit) : ChannelPipelineFactory {
    override fun getPipeline() : ChannelPipeline {
        val pipeline = DefaultChannelPipeline()
        pipeline.config ()
        return pipeline
    }
}
