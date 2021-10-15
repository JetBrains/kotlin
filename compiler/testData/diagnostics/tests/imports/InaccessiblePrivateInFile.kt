// FIR_IDENTICAL
// MODULE: m1
// FILE: f11.kt
package api

interface ApplicabilityResult

// FILE: f12.kt
package api

interface ArgumentMapping {
    fun highlightingApplicabilities(): ApplicabilityResult
}

// MODULE: m2(m1)
// FILE: f21.kt
package impl

private data class ApplicabilityResult(val applicable: Boolean)

// FILE: f22.kt
package impl

import api.*

class NullArgumentMapping : ArgumentMapping {
    // This is api.ApplicabilityResult
    override fun highlightingApplicabilities(): ApplicabilityResult = object : ApplicabilityResult {
    }
}
