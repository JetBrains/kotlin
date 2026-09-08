## Build Tools API backports

This module contains code that needs to be compiled against compiler classes, but packaged together with the Build Tools API module.


Currently, this module provides an implementation of `parseCommandLineArguments` function for the `KotlinWrapperPre2_5_0` class, to ensure 
consistent behavior of the function between versions when used in the compatibility wrapper.

### Warning - extra care must be taken when using this module!!!

In general, adding code here should be avoided unless absolutely necessary.

* all code should live in the `org.jetbrains.kotlin.buildtools.api.internal.backports` package
* code from that package should be loaded using the `org.jetbrains.kotlin.buildtools.api.internal.wrappers.BackportsClassLoader` (see `KotlinWrapperPre2_5_0`)
  * internally, the classloader will get the resource bytes (of the classes) from the parent (API) classloader and `define` the class in itself 
  * with the above setup, the classes will be loaded together with the BTA and compiler **implementation** classes (even though they are packaged into the API artifact)
* code in this module is compiled against the newest compiler version, however at runtime it might run with **older** compilers!
  * It's your responsibility to ensure that the code behaves correctly at runtime, that is that it doesn't produce missing methods or classes errors if the compiler's ABI differs! 

