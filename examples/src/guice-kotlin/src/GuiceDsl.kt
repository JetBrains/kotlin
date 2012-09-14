package org.jetbrains.kotlin.examples.guice

import com.google.inject.*
import com.google.inject.binder.*
import java.util.logging.Logger
import java.util.ArrayList

class GuiceInjectorBuilder() {
    private val collected = ArrayList<Module> ()

    fun module (config: Binder.()->Any?) : Module = object: Module {
        override fun configure(binder: Binder?) {
            binder!!.config()
        }
    }

    fun Module.plus() {
        collected.add(this)
    }

    class object {
        fun injector(config: GuiceInjectorBuilder.() -> Any?) : Injector {
            val collector = GuiceInjectorBuilder()
            collector.config()
            return Guice.createInjector(collector.collected)!!
        }
    }
}

inline fun <T> Binder.bind() = bind(javaClass<T>())!!

inline fun ScopedBindingBuilder.asSingleton() = `in`(javaClass<Singleton>())

inline fun <T> AnnotatedBindingBuilder<in T>.to() = to(javaClass<T>())!!

inline fun <T> AnnotatedBindingBuilder<in T>.toSingleton() = to(javaClass<T>())!!.asSingleton()

inline fun <T> Injector.getInstance() = getInstance(javaClass<T>())!!

inline fun <T> Injector.getProvider() = getProvider(javaClass<T>())!!

inline fun <T,S: Provider<out T>> LinkedBindingBuilder<T>.toProvider() = toProvider(javaClass<S>())

fun <T> AnnotatedBindingBuilder<T>.toProvider(provider: Injector.()->T) = toProvider(object: Provider<T> {
    [Inject] val injector : Injector? = null

    override fun get(): T = injector!!.provider()
})!!

fun <T> AnnotatedBindingBuilder<T>.toSingletonProvider(provider: Injector.()->T) = toProvider(provider).asSingleton()
