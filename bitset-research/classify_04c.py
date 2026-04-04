#!/usr/bin/env python3
"""
Step 4c use-site classification: assigns each of 422 use-site files to a usage pattern category.

Uses repo domain + file path + context line as classification signals.
Outputs a TSV with classification and a summary table.

Categories (adapted from step-03c):
 1. Dataflow / liveness analysis
 2. Set membership / visited tracking
 3. Character class / lexer automata
 4. Bytecode / IR tracking
 5. Column / parameter mask
 6. Flag / feature storage
 7. Serialization / wire protocol
 8. Graph / type-state analysis
 9. Indexing / record tracking
10. Static analysis detectors
11. Null / dirty tracking
12. Query optimization
"""

import csv
import re
from collections import defaultdict, Counter
from pathlib import Path

TSV = Path(__file__).parent / "step-04b-raw.tsv"
MD = Path(__file__).parent / "step-04b-repo-data.md"

# Category IDs
CAT_DATAFLOW = "Dataflow / liveness"
CAT_MEMBERSHIP = "Set membership / visited"
CAT_CHARCLASS = "Character class / lexer"
CAT_BYTECODE = "Bytecode / IR tracking"
CAT_MASK = "Column / parameter mask"
CAT_FLAG = "Flag / feature storage"
CAT_SERIAL = "Serialization / wire protocol"
CAT_GRAPH = "Graph / type-state"
CAT_INDEX = "Indexing / record tracking"
CAT_STATIC = "Static analysis detectors"
CAT_NULL = "Null / dirty tracking"
CAT_QUERY = "Query optimization"

ALL_CATS = [
    CAT_DATAFLOW, CAT_MEMBERSHIP, CAT_CHARCLASS, CAT_BYTECODE,
    CAT_MASK, CAT_FLAG, CAT_SERIAL, CAT_GRAPH, CAT_INDEX,
    CAT_STATIC, CAT_NULL, CAT_QUERY,
]

# ─── repo-level defaults ─────────────────────────────────────────────
REPO_DEFAULT = {
    "checkstyle": CAT_STATIC,
    "pmd": CAT_STATIC,
    "spring-framework": CAT_FLAG,
    "commons-lang": CAT_MEMBERSHIP,  # FluentBitSet is impl, the 1 use file is generic
    "eclipse-collections": CAT_MEMBERSHIP,
}


def classify(repo, path, methods, context):
    """Return (category, confidence) for a single use-site file."""
    p = path.lower()
    ctx = (context or "").lower()
    m = (methods or "").lower()

    # ── repo-level fast path ──
    if repo in REPO_DEFAULT:
        # Checkstyle: all use files are individual checks
        if repo == "checkstyle":
            return CAT_STATIC, "high"
        if repo == "pmd":
            return CAT_STATIC, "high"
        if repo == "spring-framework":
            return CAT_FLAG, "medium"
        if repo == "commons-lang":
            return CAT_MEMBERSHIP, "low"
        if repo == "eclipse-collections":
            return CAT_MEMBERSHIP, "medium"

    # ── graal (94 use files — most complex) ──
    if repo == "graal":
        if any(k in p for k in ["liveness", "livelocalvariables", "livelocalliveness",
                                  "largelocalliveness", "smalllocalliveness", "localliveness"]):
            return CAT_DATAFLOW, "high"
        if any(k in p for k in ["dataflow", "fixpointinterval", "resolvedataflow"]):
            return CAT_DATAFLOW, "high"
        if any(k in p for k in ["bciblockmapping", "bytecodeparser", "graphdecoder",
                                  "frameinfoencoder", "codeinfoencoder", "stackmapframe"]):
            return CAT_BYTECODE, "high"
        if any(k in p for k in ["replaycomp", "recordedoperation"]):
            return CAT_SERIAL, "high"
        if any(k in p for k in ["snippetparameter", "snippettemplate", "replacements",
                                  "nonnullparameter"]):
            return CAT_MASK, "high"
        if any(k in p for k in ["pointsto", "typestate", "analysispolicy",
                                  "multitypestate"]):
            return CAT_GRAPH, "high"
        if any(k in p for k in ["registerallocation", "linearscan", "constopt",
                                  "dominatoroptimization", "basicblockset",
                                  "basicblockorder", "codeemissionorder",
                                  "partialescapeclosure", "uniqueworklist",
                                  "insertproxy"]):
            return CAT_DATAFLOW, "high"
        if any(k in p for k in ["vzeroupper", "amd64"]):
            return CAT_FLAG, "medium"
        if "hotspotforeigncalllinkage" in p:
            return CAT_SERIAL, "medium"
        if "referencemap" in p:
            return CAT_SERIAL, "medium"
        if "trampolineset" in p:
            return CAT_MEMBERSHIP, "medium"
        if "layereddispatchtable" in p or "imagelayersection" in p:
            return CAT_SERIAL, "medium"
        if any(k in p for k in ["forwarddataflow", "blockiterator"]):
            return CAT_DATAFLOW, "high"
        if "graphbuilder" in p:
            return CAT_BYTECODE, "high"
        if any(k in p for k in ["launcher", "typedescriptor"]):
            return CAT_FLAG, "medium"
        if any(k in p for k in ["inlining", "callsite"]):
            return CAT_DATAFLOW, "medium"
        if any(k in p for k in ["pointstobreakdown", "dashboard"]):
            return CAT_GRAPH, "medium"
        if "defaultjavalowering" in p:
            return CAT_DATAFLOW, "medium"
        if any(k in p for k in ["pod.java"]):
            return CAT_MEMBERSHIP, "medium"
        # pass-through (type in signature)
        if "(тип в сигнатуре)" in (methods or "") or m == "":
            # Check path for hints
            if "replacements" in p or "snippet" in p:
                return CAT_MASK, "low"
            return CAT_DATAFLOW, "low"
        # fallback for graal
        if "espresso" in p:
            if "liveness" in p or "blockboundary" in p:
                return CAT_DATAFLOW, "high"
            if "graphbuilder" in p or "frameanalysis" in p or "executiongraph" in p:
                return CAT_BYTECODE, "high"
            if "arguments" in p or "libespresso" in p:
                return CAT_FLAG, "medium"
            return CAT_DATAFLOW, "low"
        return CAT_DATAFLOW, "low"

    # ── spotbugs (65 use files) ──
    if repo == "spotbugs":
        if "/detect/" in p:
            return CAT_STATIC, "high"
        if "/ba/" in p:
            return CAT_DATAFLOW, "high"
        if "opcodestack" in p or "bytecodeanalysis" in p:
            return CAT_BYTECODE, "high"
        if any(k in p for k in ["classcontext", "method.java", "javaclass"]):
            return CAT_MEMBERSHIP, "medium"
        return CAT_STATIC, "medium"

    # ── elasticsearch (45 use files) ──
    if repo == "elasticsearch":
        # Painless scripting engine — bytecode gen
        if "painless" in p:
            return CAT_BYTECODE, "high"
        # ESQL compute blocks — null mask for columnar data
        if any(k in p for k in ["compute/data/", "blockconvert", "blockfactory",
                                  "blockramusage"]):
            return CAT_NULL, "high"
        if any(k in p for k in ["esql/datasource", "ndjson"]):
            return CAT_NULL, "high"
        # SQL cursors — column mask
        if any(k in p for k in ["/sql/execution", "/sql/querydsl", "/sql/parser"]):
            return CAT_MASK, "high"
        # ESQL analysis/parsing
        if any(k in p for k in ["esql/analysis", "esql/parser", "esql/expression"]):
            return CAT_MASK, "medium"
        # EQL
        if "eql/" in p:
            return CAT_MASK, "medium"
        # Text structure — character/format detection
        if "textstructure" in p:
            return CAT_CHARCLASS, "high"
        # Feature metrics
        if "featuremetric" in p or "telemetry" in p:
            return CAT_FLAG, "high"
        # Search infrastructure
        if any(k in p for k in ["search/", "querythen"]):
            return CAT_INDEX, "medium"
        # Automaton operations
        if "automaton" in p:
            return CAT_CHARCLASS, "high"
        # Frequent itemsets
        if "frequentitemset" in p:
            return CAT_INDEX, "medium"
        # Old lucene codecs
        if "old-lucene" in p or "bwc/" in p:
            return CAT_INDEX, "medium"
        # QL tree (shared query language framework)
        if "/ql/" in p:
            return CAT_MASK, "medium"
        if "headerwarning" in p:
            return CAT_FLAG, "medium"
        return CAT_INDEX, "low"

    # ── hive (36 use files) ──
    if repo == "hive":
        if "calcite" in p or "rule" in p.split("/")[-1].lower():
            return CAT_QUERY, "high"
        if any(k in p for k in ["orc", "columnar", "vectoriz", "writablebitset"]):
            return CAT_NULL, "high"
        if any(k in p for k in ["txn", "acid", "compactor"]):
            return CAT_FLAG, "medium"
        if any(k in p for k in ["serde", "serial", "mapwork", "thrift"]):
            return CAT_SERIAL, "medium"
        if any(k in p for k in ["stat", "partition", "bucket", "prune"]):
            return CAT_QUERY, "medium"
        if any(k in p for k in ["metadata", "hmsdataschema"]):
            return CAT_SERIAL, "medium"
        return CAT_QUERY, "low"

    # ── hibernate-orm (35 use files) ──
    # Hibernate uses BitSet mostly as "which columns are included/fetched/dirty"
    if repo == "hibernate-orm":
        if "entitymetamodel" in p:
            return CAT_FLAG, "high"
        if "actionqueue" in p:
            return CAT_FLAG, "high"
        if any(k in p for k in ["null", "nullable"]):
            return CAT_NULL, "high"
        if any(k in p for k in ["dirty", "dirtiness", "dirtytrack"]):
            return CAT_NULL, "high"
        if "entitypersister" in p or "abstractentitypersister" in p:
            return CAT_NULL, "high"
        if "aggregatecolumn" in p:
            return CAT_MASK, "high"
        if any(k in p for k in ["errorhandler", "processor"]):
            return CAT_MEMBERSHIP, "medium"
        # sql/results/graph/** are pass-through column-mask propagation
        if "sql/results" in p or "sql/ast" in p:
            return CAT_MASK, "high"
        if any(k in p for k in ["sqm/sql", "query/"]):
            return CAT_MASK, "medium"
        if any(k in p for k in ["fetch", "fetchprofile", "loading", "loader"]):
            return CAT_MASK, "medium"
        if any(k in p for k in ["persister", "mutation", "tableset", "tablemapping"]):
            return CAT_MASK, "high"
        if any(k in p for k in ["spatial", "vector", "hana"]):
            return CAT_FLAG, "medium"
        if any(k in p for k in ["metamodel", "mapping", "embeddable"]):
            return CAT_MASK, "medium"
        return CAT_MASK, "low"

    # ── flink (19 use files) ──
    if repo == "flink":
        # Calcite-related files in flink-table-planner
        if "calcite" in p or "sql2rel" in p or "transposerul" in p:
            return CAT_QUERY, "high"
        if any(k in p for k in ["planner/plan/rules"]):
            return CAT_QUERY, "high"
        if any(k in p for k in ["null", "nullable", "bitmask"]):
            return CAT_NULL, "high"
        if any(k in p for k in ["serial", "typeserial", "chillpackage"]):
            return CAT_SERIAL, "medium"
        if any(k in p for k in ["updatablerowdata", "sortmergejoin", "constraint"]):
            return CAT_MASK, "high"
        if any(k in p for k in ["partitionpath"]):
            return CAT_MASK, "medium"
        # Runtime checkpoint/watermark files — tracking input/task states
        if any(k in p for k in ["checkpoint", "watermark", "inputgate", "endofdata"]):
            return CAT_FLAG, "high"
        if "runtime" in p:
            return CAT_FLAG, "medium"
        return CAT_MASK, "low"

    # ── calcite (16 use files) ──
    if repo == "calcite":
        if any(k in p for k in ["rule", "optimize", "planner"]):
            return CAT_QUERY, "high"
        if "relmeta" in p or "metadata" in p:
            return CAT_QUERY, "medium"
        if any(k in p for k in ["rex", "relbuilder", "reldecorrelator"]):
            return CAT_QUERY, "high"
        if "sql2rel" in p or "sqltoreladapter" in p or "sqltorel" in p:
            return CAT_QUERY, "high"
        return CAT_QUERY, "low"

    # ── h2database (16 use files) ──
    if repo == "h2database":
        if any(k in p for k in ["index", "scan", "page", "mvstore"]):
            return CAT_INDEX, "high"
        if "column" in p or "select" in p or "table" in p:
            return CAT_MASK, "medium"
        if any(k in p for k in ["optimizer", "plan", "condition"]):
            return CAT_QUERY, "medium"
        return CAT_INDEX, "low"

    # ── lucene (13 use files) ──
    if repo == "lucene":
        if any(k in p for k in ["chartokenizer", "edgengramtokenfilter", "lettermatcher"]):
            return CAT_CHARCLASS, "high"
        if any(k in p for k in ["codec", "posting", "docvalues", "index/"]):
            return CAT_INDEX, "high"
        if "automaton" in p or "automata" in p:
            return CAT_CHARCLASS, "high"
        if "query" in p or "scorer" in p:
            return CAT_INDEX, "medium"
        return CAT_INDEX, "low"

    # ── druid (13 use files) ──
    if repo == "druid":
        if any(k in p for k in ["bitmap", "bitset"]):
            return CAT_INDEX, "high"
        if any(k in p for k in ["filter", "selector", "column"]):
            return CAT_INDEX, "medium"
        if "query" in p:
            return CAT_QUERY, "medium"
        return CAT_INDEX, "low"

    # ── antlr4 (12 use files) ──
    if repo == "antlr4":
        if any(k in p for k in ["atn", "prediction", "predictionmode", "ambiguity",
                                  "ll1analyzer"]):
            return CAT_DATAFLOW, "high"
        if any(k in p for k in ["errorlistener", "errorstrategy", "proxyerror",
                                  "diagnosticerror", "baseerror"]):
            return CAT_MEMBERSHIP, "medium"
        if "intervalset" in p:
            return CAT_CHARCLASS, "medium"
        if "parser" in p:
            return CAT_DATAFLOW, "medium"
        return CAT_DATAFLOW, "low"

    # ── guava (6 use files) ──
    if repo == "guava":
        if "charmatcher" in p:
            return CAT_CHARCLASS, "high"
        if "smallcharmatcher" in p:
            return CAT_CHARCLASS, "high"
        if "bloom" in p:
            return CAT_MEMBERSHIP, "medium"
        return CAT_MEMBERSHIP, "low"

    # ── cassandra (5 use files) ──
    if repo == "cassandra":
        if any(k in p for k in ["sstable", "column", "index"]):
            return CAT_INDEX, "high"
        if "repair" in p or "streaming" in p:
            return CAT_SERIAL, "medium"
        return CAT_INDEX, "low"

    # ── netty (4 use files) ──
    if repo == "netty":
        if "codec" in p or "http" in p:
            return CAT_CHARCLASS, "high"
        return CAT_FLAG, "low"

    # ── beam (4 use files) ──
    if repo == "beam":
        if "trigger" in p:
            return CAT_FLAG, "medium"
        if "coder" in p or "serial" in p:
            return CAT_SERIAL, "medium"
        return CAT_MEMBERSHIP, "low"

    # ── androidx (4 use files) ──
    if repo == "androidx":
        if "parcel" in p:
            return CAT_SERIAL, "high"
        if "compose" in p or "layout" in p:
            return CAT_FLAG, "medium"
        return CAT_MEMBERSHIP, "low"

    # ── spark (1 use in MD, 0 in TSV) ──
    if repo == "spark":
        return CAT_MASK, "low"

    # ── fallback ──
    return CAT_MEMBERSHIP, "low"


def parse_md_extra_files():
    """Extract the 5 files in MD but not in TSV, classified manually."""
    # These are the 5 delta files identified: antlr4+1, graal+1, spotbugs+1, hive+1, spark+1
    # We classify them using the same logic
    extras = [
        ("antlr4", "runtime/Java/src/org/antlr/v4/runtime/Parser.java", "use", "", ""),
        ("graal", "compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/hotspot/HotSpotForeignCallLinkageImpl.java", "use", "", ""),
        ("spotbugs", "spotbugs/src/main/java/edu/umd/cs/findbugs/detect/BuildStringPassthruGraph.java", "use", "", ""),
        ("hive", "ql/src/java/org/apache/hadoop/hive/ql/optimizer/calcite/translator/opconvthunk.java", "use", "", ""),
        ("spark", "sql/catalyst/src/main/scala/org/apache/spark/sql/catalyst/expressions/codegen/CodeGenerator.scala", "use", "", ""),
    ]
    return extras


def main():
    # Parse TSV
    rows = []
    with open(TSV, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f, delimiter="\t")
        for row in reader:
            if row["cls"] != "use":
                continue
            rows.append(row)

    # Classify each row
    classifications = []
    for row in rows:
        cat, conf = classify(row["repo"], row["file"], row.get("methods", ""), row.get("context", ""))
        classifications.append({
            "repo": row["repo"],
            "file": row["file"],
            "methods": row.get("methods", ""),
            "category": cat,
            "confidence": conf,
        })

    # Add 5 MD-only files
    for repo, fpath, cls, methods, context in parse_md_extra_files():
        cat, conf = classify(repo, fpath, methods, context)
        classifications.append({
            "repo": repo,
            "file": fpath,
            "methods": methods,
            "category": cat,
            "confidence": conf,
        })

    total = len(classifications)

    # ─── Summary table ────────────────────────────────────────────────
    cat_counts = Counter(c["category"] for c in classifications)
    print(f"Total classified: {total}")
    print()
    print("### Категории use-sites")
    print()
    print("| Категория | Count | % |")
    print("|---|---:|---:|")
    for cat in ALL_CATS:
        n = cat_counts.get(cat, 0)
        pct = n / total * 100
        print(f"| {cat} | {n} | {pct:.1f} |")
    print(f"| **Итого** | **{total}** | **100.0** |")
    print()

    # ─── Per-repo breakdown ───────────────────────────────────────────
    repo_cat = defaultdict(lambda: defaultdict(int))
    for c in classifications:
        repo_cat[c["repo"]][c["category"]] += 1

    repos_order = [
        "antlr4", "graal", "lucene", "androidx", "guava", "spark",
        "spotbugs", "calcite", "h2database", "checkstyle", "pmd",
        "flink", "netty", "eclipse-collections", "druid", "hive",
        "spring-framework", "hibernate-orm", "RoaringBitmap",
        "commons-lang", "beam", "elasticsearch", "cassandra",
    ]

    # Abbreviated category names for the matrix
    cat_short = {
        CAT_DATAFLOW: "DF", CAT_MEMBERSHIP: "SM", CAT_CHARCLASS: "CL",
        CAT_BYTECODE: "BT", CAT_MASK: "CM", CAT_FLAG: "FS",
        CAT_SERIAL: "SP", CAT_GRAPH: "GT", CAT_INDEX: "IR",
        CAT_STATIC: "SD", CAT_NULL: "ND", CAT_QUERY: "QO",
    }

    print("### Per-repo breakdown")
    print()
    header = "| Repo | " + " | ".join(cat_short[c] for c in ALL_CATS) + " | Total |"
    separator = "|---|" + "|".join("---:" for _ in ALL_CATS) + "|---:|"
    print(header)
    print(separator)

    grand_total = 0
    for repo in repos_order:
        if repo not in repo_cat and repo != "RoaringBitmap":
            continue
        if repo == "RoaringBitmap":
            # 0 use files
            continue
        row_total = sum(repo_cat[repo].values())
        grand_total += row_total
        cells = []
        for cat in ALL_CATS:
            n = repo_cat[repo].get(cat, 0)
            cells.append(str(n) if n > 0 else "")
        print(f"| {repo} | " + " | ".join(cells) + f" | {row_total} |")

    print(f"| **Итого** | " + " | ".join(
        f"**{cat_counts.get(c, 0)}**" for c in ALL_CATS
    ) + f" | **{grand_total}** |")
    print()

    # ─── Confidence distribution ──────────────────────────────────────
    conf_counts = Counter(c["confidence"] for c in classifications)
    print(f"Confidence: high={conf_counts.get('high', 0)}, medium={conf_counts.get('medium', 0)}, low={conf_counts.get('low', 0)}")
    print()

    # ─── Per-category method profile ──────────────────────────────────
    print("### Per-category critical methods (top-5)")
    print()
    print("| Категория | Top-5 методов |")
    print("|---|---|")
    for cat in ALL_CATS:
        method_counts = defaultdict(int)
        for c in classifications:
            if c["category"] != cat:
                continue
            for m in (c["methods"] or "").split("; "):
                m = m.strip()
                if m:
                    method_counts[m] += 1
        top5 = sorted(method_counts.items(), key=lambda x: -x[1])[:5]
        top5_str = ", ".join(f"`{m}` ({n})" for m, n in top5)
        print(f"| {cat} | {top5_str} |")
    print()

    # ─── Write detailed TSV ───────────────────────────────────────────
    out_tsv = Path(__file__).parent / "step-04c-classified.tsv"
    with open(out_tsv, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f, delimiter="\t")
        writer.writerow(["repo", "file", "category", "confidence", "methods"])
        for c in sorted(classifications, key=lambda x: (x["repo"], x["file"])):
            writer.writerow([c["repo"], c["file"], c["category"], c["confidence"], c["methods"]])
    print(f"Detailed classification written to {out_tsv}")


if __name__ == "__main__":
    main()
