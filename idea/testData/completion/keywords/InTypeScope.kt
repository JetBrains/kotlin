fun foo() {
    val test : <caret>
}

// ABSENT: abstract
/* // ABSENT: annotation */
// EXIST: as
// EXIST: break
// EXIST: by
// EXIST: catch
// EXIST: class ... {...}
// EXIST: continue
// EXIST: do {...} while (...)
// EXIST: else
// EXIST: enum class ... {...}
// EXIST: false
// ABSENT: final
// EXIST: finally
// EXIST: for (... in ...) {...}
// EXIST: fun ...(...) : ... {...}
// EXIST: get
// EXIST: if (...) {...}
// ABSENT: import
// EXIST: in
// EXIST: inline
// EXIST: internal
// EXIST: is
// EXIST: null
// EXIST: object
// ABSENT: open
// ABSENT: out
// ABSENT: override
// ABSENT: package
// EXIST: private
// EXIST: protected
// EXIST: public
// EXIST: return
// EXIST: set
// EXIST: super
// EXIST: This
// EXIST: this
// EXIST: throw
// EXIST: trait ... {...}
// EXIST: true
// EXIST: try
// EXIST: type
// EXIST: val ... = ...
// EXIST: var ... = ...
// EXIST: vararg
// EXIST: when (...) {... -> ...else -> ...}
// EXIST: where
// EXIST: while (...) {...}
