// LANGUAGE_FEATURE_TOGGLED_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-83642
// FULL_JDK
// LANGUAGE_FEATURE_TOGGLED: LexicographicVariableReadinessCalculation

import java.sql.ResultSet
import java.sql.Types

fun <!IMPLICIT_NOTHING_RETURN_TYPE!>mapper<!>(sqlType: Int)/*: (ResultSet, Int) -> String?*/ =
    when (sqlType) {
        Types.BIGINT,
        Types.NUMERIC,
        Types.DECIMAL,
        Types.VARCHAR,
            -> ResultSet::<!NONE_APPLICABLE!>getString<!> // hangup
            //-> { rs, column -> rs.getString(column) } // ERROR: Cannot infer type for value parameter 'column'. Specify it explicitly.

        Types.TIMESTAMP,
            -> { <!CANNOT_INFER_VALUE_PARAMETER_TYPE, EXPECTED_PARAMETER_TYPE_MISMATCH!>rs<!>, <!CANNOT_INFER_VALUE_PARAMETER_TYPE, EXPECTED_PARAMETER_TYPE_MISMATCH!>column<!> -> rs.<!UNRESOLVED_REFERENCE!>getTimestamp<!>(column)?.toInstant()?.toString() }

        else -> error("Unsupported column type: $sqlType")
    }

/* GENERATED_FIR_TAGS: disjunctionExpression, equalityExpression, functionDeclaration, javaProperty, lambdaLiteral,
nullableType, safeCall, stringLiteral, whenExpression, whenWithSubject */
