package org.jetbrains.kotlin.examples.guice

import java.util.logging.Logger
import java.util.ArrayList

import com.google.inject.*
import com.google.inject.binder.*

abstract class LoggingService() {
    abstract fun info(message: String)
}

class StdoutLoggingService() : LoggingService(){
    override fun info(message: String) = println("INFO: $message")
}

class JdkLoggingService () : LoggingService() {
    protected [Inject] var jdkLogger: Logger? = null

    override fun info(message: String)  = jdkLogger.sure().info(message)
}

abstract class AppService(protected val mode: String) {
    protected abstract val logger: LoggingService

    fun run() {
        logger.info("Application started in '$mode' mode with ${logger.javaClass.getName()} logger")
        logger.info("Hello, World!")
    }
}

class RealAppService [Inject] (override val logger: LoggingService) : AppService("real")

class TestAppService (private val loggerProvider : Provider<LoggingService>) : AppService("test") {
    protected override val logger: LoggingService
        get() = loggerProvider.get()
}

fun main(args: Array<String>) {
    fun configureInjections(test: Boolean) = GuiceInjectorBuilder.injector {
        +module {
            if(test) {
                bind<LoggingService>().toInstance(StdoutLoggingService())
                bind<AppService>().toSingletonProvider{ // Injector as receiver for ext.function
                    TestAppService(getProvider<LoggingService>())
                }
            }
            else {
                bind<LoggingService>().to<JdkLoggingService>().asSingleton()
                bind<AppService>().toSingleton<RealAppService>()
            }
        }
    }

    configureInjections(test = false).getInstance<AppService>().run()
    configureInjections(test = true ).getInstance<AppService>().run()
}