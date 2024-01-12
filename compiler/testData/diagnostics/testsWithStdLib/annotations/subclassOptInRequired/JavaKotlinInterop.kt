// FILE: one.java
package pcg;

import kotlin.ExperimentalMultiplatform;
import kotlin.SubclassOptInRequired;
@SubclassOptInRequired(markerClass = ExperimentalMultiplatform.class) public class Foo{}

// FILE: two.kt

import pcg.Foo

class Bar() : Foo()


