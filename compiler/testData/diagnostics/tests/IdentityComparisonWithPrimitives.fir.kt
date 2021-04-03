val z: Boolean = true
val b: Byte = 0
val s: Short = 0
val i: Int = 0
val j: Long = 0L
val f: Float = 0.0f
val d: Double = 0.0
val c: Char = '0'

val nz: Boolean? = true
val nb: Byte? = 0
val ns: Short? = 0
val ni: Int? = 0
val nj: Long? = 0L
val nf: Float? = 0.0f
val nd: Double? = 0.0
val nc: Char? = '0'

val n: Number = 0
val nn: Number? = 0
val a: Any = 0
val na: Any? = 0

// Identity for primitive values of same type
val test_zz = z === z || z !== z
val test_bb = b === b || b !== b
val test_ss = s === s || s !== s
val test_ii = i === i || i !== i
val test_jj = j === j || j !== j
val test_ff = f === f || f !== f
val test_dd = d === d || d !== d
val test_cc = c === c || c !== c

// Identity for primitive values of different types (no extra error)
val test_zb = <!EQUALITY_NOT_APPLICABLE!>z === b<!> || <!EQUALITY_NOT_APPLICABLE!>z !== b<!>

// Primitive vs nullable
val test_znz = z === nz || nz === z || z !== nz || nz !== z
val test_bnb = b === nb || nb === b || b !== nb || nb !== b
val test_sns = s === ns || ns === s || s !== ns || ns !== s
val test_ini = i === ni || ni === i || i !== ni || ni !== i
val test_jnj = j === nj || nj === j || j !== nj || nj !== j
val test_fnf = f === nf || nf === f || f !== nf || nf !== f
val test_dnd = d === nd || nd === d || d !== nd || nd !== d
val test_cnc = c === nc || nc === c || c !== nc || nc !== c

// Primitive number vs Number
val test_bn = b === n || n === b || b !== n || n !== b
val test_sn = s === n || n === s || s !== n || n !== s
val test_in = i === n || n === i || i !== n || n !== i
val test_jn = j === n || n === j || j !== n || n !== j
val test_fn = f === n || n === f || f !== n || n !== f
val test_dn = d === n || n === d || d !== n || n !== d

// Primitive number vs Number?
val test_bnn = b === nn || nn === b || b !== nn || nn !== b
val test_snn = s === nn || nn === s || s !== nn || nn !== s
val test_inn = i === nn || nn === i || i !== nn || nn !== i
val test_jnn = j === nn || nn === j || j !== nn || nn !== j
val test_fnn = f === nn || nn === f || f !== nn || nn !== f
val test_dnn = d === nn || nn === d || d !== nn || nn !== d

// Primitive vs Any
val test_za = z === a || a === z || z !== a || a !== z
val test_ba = b === a || a === b || b !== a || a !== b
val test_sa = s === a || a === s || s !== a || a !== s
val test_ia = i === a || a === i || i !== a || a !== i
val test_ja = j === a || a === j || j !== a || a !== j
val test_fa = f === a || a === f || f !== a || a !== f
val test_da = d === a || a === d || d !== a || a !== d
val test_ca = c === a || a === c || c !== a || a !== c

// Primitive vs Any?
val test_zna = z === na || na === z || z !== na || na !== z
val test_bna = b === na || na === b || b !== na || na !== b
val test_sna = s === na || na === s || s !== na || na !== s
val test_ina = i === na || na === i || i !== na || na !== i
val test_jna = j === na || na === j || j !== na || na !== j
val test_fna = f === na || na === f || f !== na || na !== f
val test_dna = d === na || na === d || d !== na || na !== d
val test_cna = c === na || na === c || c !== na || na !== c