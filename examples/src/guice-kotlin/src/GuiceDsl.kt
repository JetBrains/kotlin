package org.jetbrains.kotlin.examples.guice

import com.google.inject.*
import com.google.inject.binder.*
import java.util.logging.Logger
import java.util.ArrayList

class GuiceInjectorBuilder() {
    private val collected = ArrayList<Module> ()

    fun module (config: Binder.()->Any?) : Module = object: Module {
        override fun configure(binder: Binder?) {
            binder.sure().config()
        }
    }

    fun Module.plus() {
        collected.add(this)
    }

    class object {
        fun injector(config: GuiceInjectorBuilder.() -> Any?) : Injector {
            val collector = GuiceInjectorBuilder()
            collector.config()
            return Guice.createInjector(collector.collected).sure()
        }
    }
}

inline fun <T> Binder.bind() = bind(javaClass<T>()).sure()

inline fun ScopedBindingBuilder.asSingleton() = `in`(javaClass<Singleton>())

inline fun <T> AnnotatedBindingBuilder<in T>.to() = to(javaClass<T>()).sure()

inline fun <T> AnnotatedBindingBuilder<in T>.toSingleton() = to(javaClass<T>()).sure().asSingleton()

inline fun <T> Injector.getInstance() = getInstance(javaClass<T>()).sure()

inline fun <T> Injector.getProvider() = getProvider(javaClass<T>()).sure()

inline fun <T,S: Provider<out T>> LinkedBindingBuilder<T>.toProvider() = toProvider(javaClass<S>())

fun <T> AnnotatedBindingBuilder<T>.toProvider(provider: Injector.()->T) = toProvider(object: Provider<T> {
    [Inject] val injector : Injector? = null

    override fun get(): T = injector.sure().provider()
}).sure()

fun <T> AnnotatedBindingBuilder<T>.toSingletonProvider(provider: Injector.()->T) = toProvider(provider).asSingleton()
