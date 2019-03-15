/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripts

import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptDefinitionsSource
import org.jetbrains.kotlin.scripting.legacy.CliScriptDefinitionProvider
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.script.templates.standard.ScriptTemplateWithArgs

class ScriptProviderTest : KtUsefulTestCase() {

    @Test
    fun testLazyScriptDefinitionsProvider() {

        val genDefCounter = AtomicInteger()
        val standardDef = FakeScriptDefinition()
        val shadedDef = FakeScriptDefinition(".x.kts")
        val provider = TestCliScriptDefinitionProvider(standardDef).apply {
            setScriptDefinitions(listOf(shadedDef, standardDef))
            setScriptDefinitionsSources(listOf(TestScriptDefinitionSource(genDefCounter, ".y.kts", ".x.kts")))
        }

        Assert.assertEquals(0, genDefCounter.get())

        provider.isScript("a.kt").let {
            Assert.assertFalse(it)
            Assert.assertEquals(0, genDefCounter.get())
        }

        provider.isScript("a.y.kts").let {
            Assert.assertTrue(it)
            Assert.assertEquals(1, genDefCounter.get())
        }

        provider.isScript("a.x.kts").let {
            Assert.assertTrue(it)
            Assert.assertEquals(1, genDefCounter.get())
            Assert.assertEquals(1, shadedDef.matchCounter.get())
        }

        provider.isScript("a.z.kts").let {
            Assert.assertTrue(it)
            Assert.assertEquals(2, genDefCounter.get())
            Assert.assertEquals(1, standardDef.matchCounter.get())
        }

        provider.isScript("a.ktx").let {
            Assert.assertFalse(it)
            Assert.assertEquals(2, genDefCounter.get())
        }
    }
}

private open class FakeScriptDefinition(val suffix: String = ".kts") : KotlinScriptDefinition(ScriptTemplateWithArgs::class) {
    val matchCounter = AtomicInteger()
    override fun isScript(fileName: String): Boolean = fileName.endsWith(suffix).also {
        if (it) matchCounter.incrementAndGet()
    }
}

private class TestScriptDefinitionSource(val counter: AtomicInteger, val defGens: Iterable<() -> FakeScriptDefinition>) :
    ScriptDefinitionsSource
{
    constructor(counter: AtomicInteger, vararg suffixes: String) : this(counter, suffixes.map { { FakeScriptDefinition(it) } })

    override val definitions: Sequence<KotlinScriptDefinition> = sequence {
        for (gen in defGens) {
            counter.incrementAndGet()
            yield(gen())
        }
    }
}

private class TestCliScriptDefinitionProvider(private val standardDef: KotlinScriptDefinition) : CliScriptDefinitionProvider() {
    override fun getDefaultScriptDefinition(): KotlinScriptDefinition = standardDef
}