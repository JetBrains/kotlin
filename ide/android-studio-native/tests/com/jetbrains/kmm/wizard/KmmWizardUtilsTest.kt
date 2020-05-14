/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard

import org.junit.Assert.*
import org.junit.Test

class KmmWizardUtilsTest {

    @Test
    fun `scan project name from settings in Kotlin`() {
        check(
            "my-application-name",
            """
            }
        }
        rootProject.name =${"\t".repeat(3)}"my-application-name"

        include(":shared")
        """
        )

        check(
            "myApplicationName",
            """
        }
        
          rootProject.name="my Application${"\t"}Name"
        include(":shared")
        """
        )

        check(
            "TheApp",
            """
        }
        
        ${"\t".repeat(3)}rootProject.name   =   "The App"
        include(":shared")
        """
        )
    }

    @Test
    fun `scan project name from settings in Groovy`() {
        check(
            "some_name",
            """
            }
        }
        rootProject.name =${"\t".repeat(3)}'some_name'

        include(":shared")
        """
        )

        check(
            "myApplicationName",
            """
        }
        
          rootProject.name='my${"\t"}Application${"\t"}Name'
        include(":shared")
        """
        )

        check(
            "TheGroovyApp",
            """
        }
        
        ${"\t".repeat(3)}rootProject.name   =   'The Groovy App'
        include(":shared")
        """
        )
    }

    private fun check(expectedName: String, fileFragment: String) {
        val actualName = scanForProjectName(fileFragment.trimIndent().split('\n'))
        assertEquals(expectedName, actualName)
    }
}