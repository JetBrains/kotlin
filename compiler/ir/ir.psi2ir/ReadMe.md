In K1 mode, this module converts resolved PSI (i.e. resolved CST) into IR.

In K2 mode, it is unused.
K2 may use PSI to parse source code, but instead of running resolution over PSI, it is first converted into FIR.
After frontend phase, analogously, FIR is converted into IR by [fir2ir](../fir/fir2ir).