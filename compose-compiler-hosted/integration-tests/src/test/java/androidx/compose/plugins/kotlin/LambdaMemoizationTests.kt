/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import android.widget.Button
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(ComposeRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class LambdaMemoizationTests : AbstractLoweringTests() {

    @Test
    fun nonCapturingEventLambda() = skipping("""
            fun eventFired() { }

            @Composable
            fun EventHolder(event: () -> Unit, block: @Composable() () -> Unit) {
              block()
            }

            @Composable
            fun Example(model: String) {
                EventHolder(event = { eventFired() }) {
                  workToBeRepeated()
                  ValidateModel(text = model)
                }
            }
        """)

    @Test
    fun lambdaInClassInitializer() = skipping("""
            @Composable
            fun EventHolder(event: () -> Unit) {
              workToBeRepeated()
            }

            @Composable
            fun Example(model: String) {
              class Nested {
                // Should not memoize the initializer
                val lambda: () -> Unit = { }
              }
              val n = Nested()
              ValidateModel(model)
              EventHolder(event = n.lambda)
            }
        """)

    @Test
    fun methodReferenceEvent() = skipping("""
            fun eventFired() { }

            @Composable
            fun EventHolder(event: () -> Unit) {}

            @Composable
            fun Example(model: String) {
              workToBeRepeated()
              EventHolder(event = ::eventFired)
            }
        """)

    @Test
    fun methodReferenceOnValue() = skipping("""
        fun eventFired(value: String) { }

        @Composable
        fun ValidateEvent(expected: String, event: () -> String) {
          val value = event()
          require(expected == value) {
            "Expected '${'$'}expected', received '${'$'}value'"
          }
        }

        @Composable
        fun Test(model: String, unchanged: String) {
          ValidateEvent(unchanged, unchanged::toString)
          ValidateEvent(model, model::toString)
        }

        @Composable
        fun Example(model: String) {
          Test(model, "unchanged")
        }
    """)

    @Test
    fun extensionMethodReferenceOnValue() = skipping("""
        fun eventFired(value: String) { }

        fun String.self() = this

        @Composable
        fun ValidateEvent(expected: String, event: () -> String) {
          val value = event()
          require(expected == value) {
            "Expected '${'$'}expected', received '${'$'}value'"
          }
        }

        @Composable
        fun Test(model: String, unchanged: String) {
          ValidateEvent(unchanged, unchanged::self)
          ValidateEvent(model, model::self)
        }

        @Composable
        fun Example(model: String) {
          Test(model, "unchanged")
        }
    """)

    @Test
    fun doNotMemoizeCallsToInlines() = skipping("""
            fun eventFired(data: String) { }

            @Composable
            fun EventHolder(event: () -> Unit, block: @Composable() () -> Unit) {
               workToBeRepeated()
               block()
            }

            @Composable
            inline fun <T, V1> inlined(value: V1, block: () -> T) = block()

            @Composable
            fun Example(model: String) {
              val e1 = inlined(model) { { eventFired(model) } }
              EventHolder(event = e1) {
                workToBeRepeated()
                ValidateModel(model)
              }
              val e2 = remember(model) { { eventFired(model) } }
              EventHolder(event = e2) {
                workToBeRepeated()
                ValidateModel(model)
              }
            }
        """)

    @Test
    fun captureParameterDirectEventLambda() = skipping("""
            fun eventFired(data: String) { }

            @Composable
            fun EventHolder(event: () -> Unit, block: @Composable() () -> Unit) {
               workToBeRepeated()
               block()
            }

            @Composable
            fun Example(model: String) {
                EventHolder(event = { eventFired(model) }) {
                  workToBeRepeated()
                  ValidateModel(text = model)
                }
            }
        """)

    @Test
    fun shouldNotRememberDirectLambdaParameter() = skipping("""
        fun eventFired(data: String) {
          println("Validating ${'$'}data")
          validateModel(data)
        }

        @Composable
        fun EventHolder(event: () -> Unit) {
          workToBeRepeated()
          event()
        }

        @Composable
        fun EventWrapper(event: () -> Unit) {
          EventHolder(event)
        }

        @Composable
        fun Example(model: String) {
          EventWrapper(event = { eventFired(model) })
        }
    """)

    @Test
    fun narrowCaptureValidation() = skipping("""
        fun eventFired(data: String) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun Example(model: String) {
          // Unmodified
          val unmodified = model.substring(0, 4)
          val modified = model + " abc"

          ExpectUnmodified(event = { eventFired(unmodified) })
          ExpectModified(event = { eventFired(modified) })
          ExpectModified(event = { eventFired(model) })
        }
    """)

    @Test
    fun captureInANestedScope() = skipping("""
        fun eventFired(data: String) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun Wrapped(block: @Composable() () -> Unit) {
          block()
        }

        @Composable
        fun Example(model: String) {
          val unmodified = model.substring(0, 4)
          val modified = model + " abc"

          Wrapped {
            ExpectUnmodified(event = { eventFired(unmodified) })
            ExpectModified(event = { eventFired(modified) })
            ExpectModified(event = { eventFired(model) })
          }
          Wrapped {
            Wrapped {
              ExpectUnmodified(event = { eventFired(unmodified) })
              ExpectModified(event = { eventFired(modified) })
              ExpectModified(event = { eventFired(model) })
            }
          }
        }
    """)

    @Test
    fun twoCaptures() = skipping("""
        fun eventFired(data: String) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun Wrapped(block: @Composable() () -> Unit) {
          block()
        }

        @Composable
        fun Example(model: String) {
          val unmodified1 = model.substring(0, 4)
          val unmodified2 = model.substring(0, 5)
          val modified1 = model + " abc"
          val modified2 = model + " abcd"

          ExpectUnmodified(event = { eventFired(unmodified1 + unmodified2) })
          ExpectModified(event = { eventFired(modified1 + unmodified1) })
          ExpectModified(event = { eventFired(unmodified2 + modified2) })
          ExpectModified(event = { eventFired(modified1 + modified2) })
        }
    """)

    @Test
    fun threeCaptures() = skipping("""
        fun eventFired(data: String) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun Example(model: String) {
          val unmodified1 = model.substring(0, 4)
          val unmodified2 = model.substring(0, 5)
          val unmodified3 = model.substring(0, 6)
          val modified1 = model + " abc"
          val modified2 = model + " abcd"
          val modified3 = model + " abcde"

          ExpectUnmodified(event = { eventFired(unmodified1 + unmodified2 + unmodified3) })
          ExpectModified(event = { eventFired(unmodified1 + unmodified2 + modified3) })
          ExpectModified(event = { eventFired(unmodified1 + modified2 + unmodified3) })
          ExpectModified(event = { eventFired(unmodified1 + modified2 + modified3) })
          ExpectModified(event = { eventFired(modified1 + unmodified2 + unmodified3) })
          ExpectModified(event = { eventFired(modified1 + unmodified2 + modified3) })
          ExpectModified(event = { eventFired(modified1 + modified2 + unmodified3) })
          ExpectModified(event = { eventFired(modified1 + modified2 + modified3) })
        }
    """)

    @Test
    fun fiveCaptures() = skipping("""
        fun eventFired(data: String) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun Example(model: String) {
          val modified = model
          val unmodified1 = model.substring(0, 1)
          val unmodified2 = model.substring(0, 2)
          val unmodified3 = model.substring(0, 3)
          val unmodified4 = model.substring(0, 4)
          val unmodified5 = model.substring(0, 5)

          ExpectUnmodified(event = { eventFired(
              unmodified1 + unmodified2 + unmodified3 + unmodified4 + unmodified1
            ) })

          ExpectModified(event = { eventFired(
              unmodified1 + unmodified2 + unmodified3 + unmodified4 + unmodified1 + modified
            ) })
        }
    """)

    @Test
    fun doNotMemoizeNonStableCaptures() = skipping("""
        val unmodifiedUnstable = Any()
        val unmodifiedString = "unmodified"

        fun eventFired(data: String) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun NonStable(model: String, nonStable: Any, unmodified: String) {
          workToBeRepeated()
          ExpectModified(event = { eventFired(nonStable.toString()) })
          ExpectModified(event = { eventFired(model) })
          ExpectUnmodified(event = { eventFired(unmodified) })
        }

        @Composable
        fun Example(model: String) {
          NonStable(model, unmodifiedUnstable, unmodifiedString)
        }
    """)

    @Test
    fun doNotMemoizeVarCapures() = skipping("""
        fun eventFired(data: Int) { }

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        @Composable
        fun Wrap(block: @Composable() () -> Unit) {
          block()
        }

        @Composable
        fun Test(model: String, b: Int) {
          var a = 1
          var c = false
          ExpectModified(event = { a++ })
          ExpectModified(event = { eventFired(a) })
          ExpectModified(event = { c = true })
          ExpectUnmodified(event = { eventFired(b) })
          Wrap {
            ExpectModified(event = { a++ })
            ExpectModified(event = { eventFired(a) })
            ExpectModified(event = { c = true })
            ExpectUnmodified(event = { eventFired(b) })
          }
        }

        @Composable
        fun Example(model: String) {
          Test(model, 1)
        }
    """)

    @Test
    fun considerNonComposableCaptures() = skipping("""
        fun eventFired(data: Int) {}

        @Composable
        fun ExpectUnmodified(event: () -> Unit) {
          workToBeAvoided()
        }

        @Composable
        fun ExpectModified(event: () -> Unit) {
          workToBeRepeated()
        }

        inline fun wrap(value: Int, block: (value: Int) -> Unit) {
          block(value)
        }

        @Composable
        fun Example(model: String) {
           wrap(iterations) { number ->
             ExpectModified(event = { eventFired(number) })
             ExpectUnmodified(event = { eventFired(5) })
           }
        }
    """)

    private fun skipping(text: String, dumpClasses: Boolean = false) =
        ensureSetup {
            compose("""
                var avoidedWorkCount = 0
                var repeatedWorkCount = 0
                var expectedAvoidedWorkCount = 0
                var expectedRepeatedWorkCount = 0

                fun workToBeAvoided(msg: String = "") {
                   avoidedWorkCount++
                   println("Work to be avoided ${'$'}avoidedWorkCount ${'$'}msg")
                }
                fun workToBeRepeated(msg: String = "") {
                   repeatedWorkCount++
                   println("Work to be repeated ${'$'}repeatedWorkCount ${'$'}msg")
                }

                $text

                @Composable
                fun Display(text: String) {}

                fun validateModel(text: String) {
                  require(text == "Iteration ${'$'}iterations")
                }

                @Composable
                fun ValidateModel(text: String) {
                  validateModel(text)
                }

                @Composable
                fun TestHost() {
                   println("START: Iteration - ${'$'}iterations")
                   Button(id=42, onClick=invalidate)
                   Example("Iteration ${'$'}iterations")
                   println("END  : Iteration - ${'$'}iterations")
                   validate()
                }

                var iterations = 0

                fun validate() {
                  if (iterations++ == 0) {
                    expectedAvoidedWorkCount = avoidedWorkCount
                    expectedRepeatedWorkCount = repeatedWorkCount
                    repeatedWorkCount = 0
                  } else {
                    require(expectedAvoidedWorkCount == avoidedWorkCount) {
                      "Executed avoided work unexpectedly, expected " +
                      "${'$'}expectedAvoidedWorkCount" +
                      ", received ${'$'}avoidedWorkCount"
                    }
                    require(expectedRepeatedWorkCount == repeatedWorkCount) {
                      "Expected more repeated work, expected ${'$'}expectedRepeatedWorkCount" +
                      ", received ${'$'}repeatedWorkCount"
                    }
                    repeatedWorkCount = 0
                  }
                }

            """, """
                TestHost()
            """, dumpClasses = dumpClasses).then { activity ->
                val button = activity.findViewById(42) as Button
                button.performClick()
            }.then { activity ->
                val button = activity.findViewById(42) as Button
                button.performClick()
            }.then {
                // Wait for test to complete
            }
        }
}
