// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// FILE: pcg/Foo.java
package pcg;

import kotlin.ExperimentalMultiplatform;
import kotlin.SubclassOptInRequired;

@SubclassOptInRequired(markerClass = ExperimentalMultiplatform.class)
public class Foo {}

// FILE: two.kt
import pcg.Foo

class Bar() : <!OPT_IN_TO_INHERITANCE_ERROR!>Foo<!>()
