# Шаг 3c, часть 2. Стабильность top-10 и классификация паттернов use-sites

## Резюме

Сравнение `Kotlin-only` и `Combined` показало, что top-10 неустойчив: top-5 membership уже меняется (`or()` выпадает, `set(from,to)` входит), а несколько позиций смещаются намного больше чем на 1 ранг. Быстрый добор двух открытых JetBrains-репозиториев (`JetBrains/Grammar-Kit`, `JetBrains/markdown`) добавил 5 релевантных use-sites, но top-10 и top-5 membership не изменил; итоговый статус — `не стабилизировалась`.

Все `178` use-sites (`158` из IntelliJ TSV + `20` из Kotlin) классифицированы ровно в одну категорию. Наибольшие кластеры — `Dataflow / liveness analysis` (`35` файлов), `Diff / text comparison` (`27`) и `Bytecode offset tracking` (`26`); совпадающих method profiles, требующих слияния категорий, не обнаружено.

## Входные данные

- `bitset-research/step-03c-part1-frequencies.md`
- `bitset-research/step-03b-extracted.tsv`
- `bitset-research/step-03a-kotlin-repo-data.md`

## 1. Оценка стабильности

### 1.1 Top-10: Kotlin-only vs Combined

| Rank | Kotlin-only | Count | Combined | Count |
|---|---|---:|---|---:|
| 1 | `get(int)` | 13 | `get(int)` | 75 |
| 2 | `or()` | 10 | `set(from,to)` | 63 |
| 3 | `BitSet(int)` | 9 | `BitSet()` | 60 |
| 4 | `set(int)` | 8 | `set(int)` | 52 |
| 5 | `BitSet()` | 7 | `BitSet(int)` | 33 |
| 6 | `copy()` | 7 | `set(int,bool)` | 32 |
| 7 | `forEachBit()` | 6 | `nextSetBit()` | 29 |
| 8 | `set(int,bool)` | 6 | `or()` | 21 |
| 9 | `equals()` | 5 | `isEmpty()` | 18 |
| 10 | `and()` | 4 | `cardinality()` | 16 |

### 1.2 Проверка критерия

- `top-5 membership` не совпал. Kotlin-only: `get(int)`, `or()`, `BitSet(int)`, `set(int)`, `BitSet()`. Combined: `get(int)`, `set(from,to)`, `BitSet()`, `set(int)`, `BitSet(int)`.
- Уже на уровне лидеров есть сильные сдвиги рангов: `or()` `2 -> 8`, `copy()` `6 -> 16`, `forEachBit()` `7 -> 18`, `and()` `10 -> 23`.
- Формальный вывод по базовой выборке: `нестабильна`.

### 1.3 Быстрый добор двух открытых JetBrains-репозиториев

#### JetBrains/Grammar-Kit

- Включено use-sites: `3`.
- Исключено: `1`.
- Included files: `src/org/intellij/grammar/generator/RuleGraphHelper.java` -> `BitSet(int)`, `set(from,to,bool)`, `isEmpty()`, `get(int)`, `set(int,bool)`, `cardinality()`; `gen/org/intellij/jflex/parser/_JFlexLexer.java` -> `BitSet(int)`, `size()`, `set(int,bool)`, `clear(int)`, `get(int)`; `src/org/intellij/grammar/livePreview/LivePreviewParser.java` -> `BitSet(int)`, `get(int)`, `set(int)`, `clear(int)`.
- Excluded files: `antlr-based-bootstrap/peg/GrammarParser.java (uses org.antlr.runtime.BitSet, not java.util.BitSet)`.

#### JetBrains/markdown

- Включено use-sites: `2`.
- Исключено: `5`.
- Included files: `src/commonMain/kotlin/org/intellij/markdown/flavours/gfm/lexer/_GFMLexer.kt` -> `BitSet(int)`, `set(int,bool)`, `clear(int)`, `get(int)`; `src/commonMain/kotlin/org/intellij/markdown/flavours/space/lexer/_SFMLexer.kt` -> `BitSet(int)`, `set(int,bool)`, `get(int)`.
- Excluded files: `src/commonMain/kotlin/org/intellij/markdown/html/CommonDefs.kt (expect declaration)`; `src/jvmMain/kotlin/CommonDefsImplJvm.kt (implementation)`; `src/jsMain/kotlin/CommonDefsImplJs.kt (typealias implementation)`; `src/wasmJsMain/kotlin/CommonDefsImplJs.kt (typealias implementation)`; `src/nativeMain/kotlin/org/intellij/markdown/html/CommonDefsImplNative.kt (typealias implementation)`.

Совокупный вклад двух репозиториев в частоты методов:

| Rank | Method | Extra count |
|---|---|---:|
| 1 | `BitSet(int)` | 5 |
| 2 | `get(int)` | 5 |
| 3 | `set(int,bool)` | 4 |
| 4 | `clear(int)` | 3 |
| 5 | `cardinality()` | 1 |
| 6 | `isEmpty()` | 1 |
| 7 | `set(from,to,bool)` | 1 |
| 8 | `set(int)` | 1 |
| 9 | `size()` | 1 |

### 1.4 Пересчёт после добора

| Rank | Combined + 2 repos | Count | Delta vs Combined |
|---|---|---:|---:|
| 1 | `get(int)` | 80 | +5 |
| 2 | `set(from,to)` | 63 | +0 |
| 3 | `BitSet()` | 60 | +0 |
| 4 | `set(int)` | 53 | +1 |
| 5 | `BitSet(int)` | 38 | +5 |
| 6 | `set(int,bool)` | 36 | +4 |
| 7 | `nextSetBit()` | 29 | +0 |
| 8 | `or()` | 21 | +0 |
| 9 | `isEmpty()` | 19 | +1 |
| 10 | `cardinality()` | 17 | +1 |

### 1.5 Итог по стабильности

- Статус: `не стабилизировалась`.
- После добора `top-5 membership` всё ещё отличается от Kotlin-only: `get(int)`, `set(from,to)`, `BitSet()`, `set(int)`, `BitSet(int)`.
- Порядок expanded top-10 полностью совпал с исходным `Combined`, то есть добор добавил массу в уже доминирующие методы (`get(int)`, `BitSet(int)`, `set(int,bool)`), но не изменил профиль лидеров.

## 2. Классификация use-sites по паттернам

### 2.1 Методология

- База классификации: все `cls=use` строки из `step-03b-extracted.tsv` плюс `20` Kotlin use-sites из `step-03c-part1-frequencies.md`.
- Каждому use-site назначена ровно одна категория по контексту файла; pass-through entries классифицированы по роли файла, а не по отсутствующим методам.
- `method profile` считался как множество методов, встречающихся в файлах категории; идентичных профилей у разных категорий нет, поэтому слияния не потребовалось.

### 2.2 Сводка по категориям

| Категория | Count | Critical methods |
|---|---:|---|
| Dataflow / liveness analysis | 35 | `get(int)` (23), `BitSet()` (18), `set(int)` (17), `nextSetBit()` (10), `or()` (9) |
| Set membership / visited tracking | 10 | `BitSet(int)` (5), `BitSet()` (4), `get(int)` (3), `or()` (3), `isEmpty()` (3) |
| Diff / text comparison | 27 | `set(from,to)` (19), `BitSet()` (10), `nextSetBit()` (8), `nextClearBit()` (5), `get(int)` (4) |
| Character class / regex | 5 | `get(int)` (4), `BitSet()` (2), `BitSet(int)` (2), `set(int,bool)` (2), `nextSetBit()` (2) |
| Bytecode offset tracking | 26 | `set(from,to)` (16), `BitSet()` (7), `nextSetBit()` (5), `get(int)` (3), `set(int)` (3) |
| Parameter mask / overload generation | 8 | `get(int)` (5), `BitSet(int)` (4), `set(int)` (2), `isEmpty()` (2), `length()` (2) |
| Flag storage | 16 | `get(int)` (8), `set(int,bool)` (7), `set(from,to)` (7), `BitSet()` (6), `set(int)` (4) |
| Serialization protocol | 9 | `BitSet()` (4), `set(from,to)` (3), `set(int)` (2), `BitSet(int)` (2), `set(int,bool)` (2) |
| Graph algorithms | 20 | `get(int)` (11), `set(int,bool)` (9), `set(from,to)` (8), `BitSet()` (4), `forEachBit()` (2) |
| Indexing / file ID containers | 22 | `set(int)` (16), `get(int)` (13), `clear()` (6), `BitSet()` (5), `size()` (5) |

### 1. Dataflow / liveness analysis

- Count: `35`.
- Critical methods: `get(int)` (23), `BitSet()` (18), `set(int)` (17), `nextSetBit()` (10), `or()` (9), `cardinality()` (6), `isEmpty()` (6), `BitSet(int)` (6).
- Files:
- `compiler/backend/src/org/jetbrains/kotlin/codegen`: `coroutines/resumePointDependentAnalysis.kt`, `optimization/common/variableLiveness.kt`
- `compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/lower/optimizations`: `LivenessAnalysis.kt`
- `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations`: `ComputeTypesPass.kt`, `StaticInitializersOptimization.kt`
- `platform/analysis-impl/src/com/intellij/codeInspection/dataFlow`: `interpreter/ReachabilityCountingInterpreter.java`, `lang/ir/ControlFlow.java`, `lang/ir/LiveVariablesAnalyzer.java`, `memory/DfaMemoryStateImpl.java`, `memory/SortedIntSet.java`, `rangeSet/LongRangeSet.java`
- `java/java-analysis-impl/src/com/intellij/codeInspection`: `bytecodeAnalysis/ProjectBytecodeAnalysis.java`, `dataFlow/inference/ContractInferenceIndex.kt`, `dataFlow/inference/ContractInferenceInterpreter.java`, `dataFlow/inference/JavaSourceInference.java`, `dataFlow/inference/ParameterNullityInference.kt`, `dataFlow/inference/inferenceResults.kt (pass-through)`
- `java/java-analysis-impl/src/com/siyeh/ig`: `controlflow/DuplicateConditionInspection.java`, `migration/TryFinallyCanBeTryWithResourcesInspection.java`, `psiutils/ControlFlowUtils.java`
- `java/java-impl-inspections/src/com/intellij/codeInspection/defUse`: `DefUseInspection.java`
- `java/java-impl-refactorings/src/com/intellij/refactoring/extractMethod`: `ExtractMethodRecommenderInspection.java`
- `java/java-psi-impl/src/com/intellij/psi/controlFlow`: `ControlFlowUtil.java`, `DefUseUtil.java`
- `plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy`: `codeInspection/utils/ControlFlowUtils.java`, `lang/psi/controlFlow/ControlFlowBuilderUtil.java`, `lang/psi/dataFlow/WorkList.kt`, `lang/psi/dataFlow/readWrite/ReadBeforeWriteInstance.kt`, `lang/psi/dataFlow/readWrite/ReadBeforeWriteState.kt`, `lang/psi/dataFlow/types/InferenceCache.java`, `lang/psi/dataFlow/types/TypeDfaState.java`, `lang/psi/dataFlow/util.kt`
- `plugins/groovy/src/org/jetbrains/plugins/groovy/refactoring`: `inline/GroovyInlineLocalHandler.java`, `inline/GroovyInlineLocalProcessor.java`, `introduce/parameter/GrIntroduceClosureParameterProcessor.java`

### 2. Set membership / visited tracking

- Count: `10`.
- Critical methods: `BitSet(int)` (5), `BitSet()` (4), `get(int)` (3), `or()` (3), `isEmpty()` (3), `set(int)` (3), `cardinality()` (2), `size()` (2).
- Files:
- `compiler/util/src/org/jetbrains/kotlin/utils`: `BitSetUtil.kt`
- `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations`: `CastsOptimization.kt`
- `fleet/util/core/srcCommonMain/fleet/util`: `BitSet.kt`
- `platform/analysis-impl/src/com/intellij/codeInsight/template/impl`: `TemplateState.java`
- `java/idea-ui/src/com/intellij/ide/util/importProject`: `RootDetectionProcessor.java`
- `platform/lang-impl/src/com/intellij/codeInsight/editorActions/wordSelection`: `InjectedFileReferenceSelectioner.java`
- `platform/util/ui/src/com/intellij/util/ui`: `ExtendableHTMLViewFactory.kt`, `html/utils.kt (pass-through)`
- `platform/syntax/syntax-api/src/com/intellij/platform/syntax`: `SyntaxElementTypeSet.kt`, `impl/util/MutableBitSet.kt (pass-through)`

### 3. Diff / text comparison

- Count: `27`.
- Critical methods: `set(from,to)` (19), `BitSet()` (10), `nextSetBit()` (8), `nextClearBit()` (5), `get(int)` (4), `BitSet(int)` (4), `set(from,to,bool)` (4), `set(int,bool)` (3).
- Files:
- `java/java-impl/src/com/intellij/codeInsight/javadoc`: `SnippetMarkup.java`
- `platform/util/diff/src/com/intellij/util/diff`: `Diff.kt (pass-through)`, `MyersLCS.kt`, `PatienceIntLCS.kt`, `Reindexer.kt`
- `platform/diff-impl/src/com/intellij/diff`: `comparison/ComparisonManagerImpl.java`, `merge/MergeThreesideViewerActions.kt (pass-through)`, `tools/combined/CombinedDiffViewer.kt`, `tools/fragmented/UnifiedDiffViewer.java (pass-through)`, `tools/simple/SimpleDiffViewer.java (pass-through)`, `tools/simple/SimpleThreesideDiffViewer.java (pass-through)`, `tools/util/text/SmartTextDiffProvider.java`, `util/DiffUtil.java`
- `platform/diff-impl/src/com/intellij/openapi/vcs/ex`: `DocumentTracker.kt`, `LineStatusTrackerBase.kt`, `LineStatusTrackerBlockOperations.kt`, `LineStatusTrackerI.kt`
- `platform/vcs-impl/lang/src/com/intellij/codeInsight/actions`: `VcsFacadeImpl.java`
- `platform/vcs-impl/src/com/intellij/openapi/vcs/changes/actions/diff/lst`: `LocalTrackerDiffUtil.kt`, `UnifiedLocalChangeListDiffViewer.kt`
- `platform/vcs-impl/src/com/intellij/openapi/vcs/changes/patch`: `AppliedTextPatch.java`, `tool/ApplyPatchViewer.java (pass-through)`
- `platform/vcs-impl/src/com/intellij/openapi/vcs/ex`: `MoveChangesLineStatusAction.java`, `PartialLocalLineStatusTracker.kt`, `RollbackLineStatusAction.java (pass-through)`
- `platform/vcs-impl/src/com/intellij/openapi/vcs/impl`: `ElementStatusTrackerImpl.java`
- `plugins/git4idea/src/git4idea/index`: `GitStageLineStatusTracker.kt`

### 4. Character class / regex

- Count: `5`.
- Critical methods: `get(int)` (4), `BitSet()` (2), `BitSet(int)` (2), `set(int,bool)` (2), `nextSetBit()` (2), `set(int)` (1), `clear(int)` (1), `size()` (1).
- Files:
- `compiler/psi/parser/src/org/jetbrains/kotlin/kdoc/lexer`: `_KDocLexer.java`
- `libraries/stdlib/native-wasm/src/kotlin/text/regex`: `AbstractCharClass.kt`, `CharClass.kt`
- `platform/util/text-matching/src/com/intellij/psi/codeStyle`: `TypoTolerantMatcher.kt`
- `platform/util/text-matching/srcJvm/com/intellij/util/text/matching`: `BitSet.kt (pass-through)`

### 5. Bytecode offset tracking

- Count: `26`.
- Critical methods: `set(from,to)` (16), `BitSet()` (7), `nextSetBit()` (5), `get(int)` (3), `set(int)` (3), `or()` (3), `length()` (3), `isEmpty()` (2).
- Files:
- `compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/boxing`: `PopBackwardPropagationTransformer.kt`
- `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler`: `ConcatenationHelper.java (pass-through)`, `ExprProcessor.java`, `SimplifyExprentsHelper.java (pass-through)`
- `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps`: `AnnotationExprent.java`, `ArrayExprent.java`, `AssertExprent.java`, `AssignmentExprent.java`, `ConstExprent.java`, `ExitExprent.java`, `Exprent.java`, `FieldExprent.java`, `FunctionExprent.java`, `IfExprent.java`, `MonitorExprent.java`, `NewExprent.java`, `SwitchExprent.java`, `VarExprent.java`
- `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/stats`: `CatchStatement.java (pass-through)`, `DummyExitStatement.java`, `Statement.java`
- `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/struct`: `attr/StructLocalVariableTableAttribute.java`, `consts/ConstantPool.java`
- `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/main`: `collectors/BytecodeMappingTracer.java`, `rels/LambdaProcessor.java`
- `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/util`: `DebugPrinter.java`

### 6. Parameter mask / overload generation

- Count: `8`.
- Critical methods: `get(int)` (5), `BitSet(int)` (4), `set(int)` (2), `isEmpty()` (2), `length()` (2), `clone()` (2), `equals()` (2), `set(from,to)` (1).
- Files:
- `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol`: `classes/symbolLightClassUtils.kt`, `methods/SymbolLightConstructor.kt (pass-through)`, `methods/SymbolLightMethod.kt`, `methods/SymbolLightSimpleMethod.kt (pass-through)`, `symbolLightUtils.kt`
- `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps`: `InvocationExprent.java`
- `plugins/kotlin/code-insight/fixes-k2/src/org/jetbrains/kotlin/idea/k2/codeinsight/fixes`: `ChangeMemberFunctionSignatureFixFactory.kt`
- `plugins/kotlin/idea/src/org/jetbrains/kotlin/idea/quickfix`: `ChangeMemberFunctionSignatureFix.kt`

### 7. Flag storage

- Count: `16`.
- Critical methods: `get(int)` (8), `set(int,bool)` (7), `set(from,to)` (7), `BitSet()` (6), `set(int)` (4), `BitSet(int)` (4), `equals()` (2), `clear(int)` (1).
- Files:
- `native/commonizer/src/org/jetbrains/kotlin/commonizer/stats`: `RawStatsCollector.kt`
- `grid/impl/src/run/ui`: `HiddenColumnsSelectionHolder.java`
- `platform/core-impl/src/com/intellij/lang/impl`: `MarkerOptionalData.java`
- `platform/core-impl/src/com/intellij/openapi/editor/impl`: `PsiBasedStripTrailingSpacesFilter.java`
- `platform/platform-api/src/com/intellij/ui`: `OptionGroup.java`
- `platform/platform-impl/initial-config-import/src/com/intellij/ide`: `OldDirectoryCleaner.java`
- `platform/platform-impl/src/com/intellij/openapi/actionSystem/impl`: `PreCachedDataContext.kt`
- `platform/syntax/syntax-api/src/com/intellij/platform/syntax/impl/builder`: `MarkerOptionalData.kt`
- `platform/vcs-impl/shared/src/com/intellij/openapi/vcs/changes/ui`: `ChangeListRemoteState.kt`
- `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/vars`: `VarProcessor.java`
- `plugins/kotlin/idea/src/org/jetbrains/kotlin/idea/refactoring/move/moveDeclarations/ui`: `MoveKotlinNestedClassesToUpperLevelDialog.java`, `MoveKotlinTopLevelDeclarationsDialog.java`
- `updater/src/mslinks/data`: `BitSet32.java (pass-through)`, `CNRLinkFlags.java (pass-through)`, `LinkFlags.java (pass-through)`, `LinkInfoFlags.java (pass-through)`

### 8. Serialization protocol

- Count: `9`.
- Critical methods: `BitSet()` (4), `set(from,to)` (3), `set(int)` (2), `BitSet(int)` (2), `set(int,bool)` (2), `toByteArray()` (1), `nextSetBit()` (1), `get(int)` (1).
- Files:
- synthetic entries: `API dumps (7 файлов) (pass-through)`, `JDK API version files (4 файла) (pass-through)`, `JDK аннотации (1 файл) (pass-through)`
- `java/java-analysis-impl/src/com/intellij/codeInspection/dataFlow/inference`: `MethodDataExternalizer.kt`
- `platform/indexing-impl/src/com/intellij/psi/stubs`: `LazyStubList.java`, `StubTreeSerializerBase.java`
- `python/python-psi-api/src/com/jetbrains/python/psi/stubs`: `PyFileStub.java (pass-through)`
- `python/python-psi-impl/src/com/jetbrains/python/psi`: `PyFileElementType.java`, `impl/stubs/PyFileStubImpl.java`

### 9. Graph algorithms

- Count: `20`.
- Critical methods: `get(int)` (11), `set(int,bool)` (9), `set(from,to)` (8), `BitSet()` (4), `forEachBit()` (2), `set(int)` (2), `or()` (2), `clone()` (1).
- Files:
- `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations`: `CallGraphBuilder.kt`, `DataFlowIR.kt`, `DevirtualizationAnalysis.kt`
- `platform/vcs-log/graph/src/com/intellij/vcs/log/graph/collapsing`: `CollapsedActionManager.java`, `CollapsedController.java (pass-through)`, `CollapsedGraph.java`, `GraphNodesVisibility.java`
- `platform/vcs-log/graph/src/com/intellij/vcs/log/graph/impl/facade`: `FirstParentController.kt`, `ReachableNodes.kt (pass-through)`, `sort/bek/BekBranchCreator.java`
- `platform/vcs-log/graph/src/com/intellij/vcs/log/graph/impl/permanent`: `PermanentLinearGraphBuilder.java`
- `platform/vcs-log/graph/src/com/intellij/vcs/log/graph/utils`: `BfsUtil.kt`, `DfsUtil.kt`, `GraphUtil.kt`, `UnsignedBitSet.java (pass-through)`, `impl/BitSetFlags.java (pass-through)`
- `platform/vcs-log/impl/src/com/intellij/vcs/log/history`: `FileHistoryRefiner.kt`
- `plugins/git4idea/intellij.vcs.git.coverage/src/com/intellij/vcs/git/coverage`: `CurrentFeatureBranchBaseDetector.kt`
- `plugins/git4idea/src/git4idea/history`: `GitHistoryTraverserImpl.kt (pass-through)`
- `plugins/git4idea/src/git4idea/rebase/log`: `GitCommitEditingActionBase.kt (pass-through)`

### 10. Indexing / file ID containers

- Count: `22`.
- Critical methods: `set(int)` (16), `get(int)` (13), `clear()` (6), `BitSet()` (5), `size()` (5), `clear(int)` (4), `cardinality()` (4), `BitSet(int)` (3).
- Files:
- `platform/util/src/com/intellij/util/indexing/containers`: `BitSetAsRAIntContainer.java`, `ChangeBufferingList.java`, `SortedIdSet.java`
- `platform/indexing-api/src/com/intellij/util/indexing`: `IdFilter.java`, `roots/IndexableFilesDeduplicateFilter.java`
- `platform/lang-api/src/com/intellij/util/indexing`: `LightDirectoryIndex.java`
- `platform/lang-impl/src/com/intellij/find/impl`: `FindInProjectTask.java`
- `platform/lang-impl/src/com/intellij/openapi/roots/impl`: `FilesScanExecutor.kt`
- `platform/lang-impl/src/com/intellij/psi/search`: `MappedFileTypeIndex.java (pass-through)`
- `platform/lang-impl/src/com/intellij/util/indexing`: `contentQueue/dev/IndexWriter.kt`, `events/DirtyFiles.kt`, `impl/storage/IntLog.kt`, `projectFilter/CachingProjectIndexableFilesFilter.kt`, `projectFilter/ConcurrentFileIds.kt`, `projectFilter/ProjectIndexableFilesFilterHealthCheck.kt`
- `platform/core-impl/src/com/intellij/openapi/vfs`: `CompactVirtualFileSet.java`, `DeduplicatingVirtualFileFilter.kt`
- `platform/platform-impl/src/com/intellij/openapi/fileTypes/impl`: `IgnoredFileCache.java`
- `platform/platform-impl/src/com/intellij/openapi/vfs/newvfs`: `impl/VfsData.java`, `persistent/CompactRecordsTable.java`
- `platform/projectModel-impl/src/com/intellij/workspaceModel/core/fileIndex/impl`: `WorkspaceFileIndexDataImpl.kt`
- `platform/todo/src/com/intellij/psi/impl/cache/impl`: `IndexTodoCacheManagerImpl.java`

## 3. Верификация

- `Kotlin use-sites`: `20`.
- `IntelliJ TSV use-sites`: `158`.
- `Total classified`: `178`.
- `Method-bearing use-sites`: `147`; `pass-through`: `31`.
- Проверка `total classified = total use-sites из part1` выполнена: `20 + 158 = 178`.
- Неклассифицированных записей нет; дублирующих присвоений нет.
- Слияние категорий не потребовалось: совпадающих method profiles не найдено.
