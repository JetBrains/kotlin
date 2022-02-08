const val byteMax = <!EVALUATED: `127`!>{ java.lang.Byte.MAX_VALUE }()<!>
const val byteMin = <!EVALUATED: `-128`!>{ java.lang.Byte.MIN_VALUE }()<!>
const val byteSize = <!EVALUATED: `8`!>{ java.lang.Byte.SIZE }()<!>
const val byteBytes = <!EVALUATED: `1`!>{ java.lang.Byte.BYTES }()<!>

const val intMax = <!EVALUATED: `2147483647`!>{ java.lang.Integer.MAX_VALUE }()<!>
const val intMin = <!EVALUATED: `-2147483648`!>{ java.lang.Integer.MIN_VALUE }()<!>
const val intSize = <!EVALUATED: `32`!>{ java.lang.Integer.SIZE }()<!>
const val intBytes = <!EVALUATED: `4`!>{ java.lang.Integer.BYTES }()<!>

const val floatMax = <!EVALUATED: `3.4028235E38`!>{ java.lang.Float.MAX_VALUE }()<!>
const val floatMin = <!EVALUATED: `1.4E-45`!>{ java.lang.Float.MIN_VALUE }()<!>
const val floatSize = <!EVALUATED: `32`!>{ java.lang.Float.SIZE }()<!>
const val floatBytes = <!EVALUATED: `4`!>{ java.lang.Float.BYTES }()<!>
const val floatMaxExponent = <!EVALUATED: `127`!>{ java.lang.Float.MAX_EXPONENT }()<!>
const val floatMinExponent = <!EVALUATED: `-126`!>{ java.lang.Float.MIN_EXPONENT }()<!>
const val floatNegInfinity = <!EVALUATED: `-Infinity`!>{ java.lang.Float.NEGATIVE_INFINITY }()<!>
const val floatPosInfinity = <!EVALUATED: `Infinity`!>{ java.lang.Float.POSITIVE_INFINITY }()<!>

const val booleanFalse = { java.lang.Boolean.FALSE }().<!EVALUATED: `false`!>toString()<!>
const val booleanTrue = { java.lang.Boolean.TRUE }().<!EVALUATED: `true`!>toString()<!>

const val charMax = { java.lang.Character.MAX_VALUE }().<!EVALUATED: `65535`!>toInt()<!>
const val charMin = { java.lang.Character.MIN_VALUE }().<!EVALUATED: `0`!>toInt()<!>
const val charSize = <!EVALUATED: `16`!>{ java.lang.Character.SIZE }()<!>
const val charBytes = <!EVALUATED: `2`!>{ java.lang.Character.BYTES }()<!>
