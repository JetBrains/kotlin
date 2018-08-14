#include <stdint.h>
#include <jni.h>
#include <clang-c/Index.h>
#include <clang-c/ext.h>

// NOTE THIS FILE IS AUTO-GENERATED

JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge0 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_getCString(*(CXString*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge1 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_disposeString(*(CXString*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge2 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_disposeStringSet((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge3 (JNIEnv* jniEnv, jclass jclss) {
    return (jlong)clang_getBuildSessionTimestamp();
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge4 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jlong)clang_VirtualFileOverlay_create(p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge5 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jint)clang_VirtualFileOverlay_addFileMapping((void*)p0, (void*)p1, (void*)p2);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge6 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jint)clang_VirtualFileOverlay_setCaseSensitivity((void*)p0, p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge7 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3) {
    return (jint)clang_VirtualFileOverlay_writeToBuffer((void*)p0, p1, (void*)p2, (void*)p3);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge8 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_free((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge9 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_VirtualFileOverlay_dispose((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge10 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jlong)clang_ModuleMapDescriptor_create(p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge11 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_ModuleMapDescriptor_setFrameworkModuleName((void*)p0, (void*)p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge12 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_ModuleMapDescriptor_setUmbrellaHeader((void*)p0, (void*)p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge13 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3) {
    return (jint)clang_ModuleMapDescriptor_writeToBuffer((void*)p0, p1, (void*)p2, (void*)p3);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge14 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_ModuleMapDescriptor_dispose((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge15 (JNIEnv* jniEnv, jclass jclss, jint p0, jint p1) {
    return (jlong)clang_createIndex(p0, p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge16 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_disposeIndex((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge17 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    clang_CXIndex_setGlobalOptions((void*)p0, p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge18 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_CXIndex_getGlobalOptions((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge19 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    clang_CXIndex_setInvocationEmissionPathOption((void*)p0, (void*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge20 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_getFileName((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge21 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_getFileTime((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge22 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_getFileUniqueID((void*)p0, (void*)p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge23 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_isFileMultipleIncludeGuarded((void*)p0, (void*)p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge24 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)clang_getFile((void*)p0, (void*)p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge25 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)clang_getFileContents((void*)p0, (void*)p1, (void*)p2);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge26 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_File_isEqual((void*)p0, (void*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge27 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    *(CXSourceLocation*)p0 = clang_getNullLocation();
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge28 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_equalLocations(*(CXSourceLocation*)p0, *(CXSourceLocation*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge29 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jint p3, jlong p4) {
    *(CXSourceLocation*)p4 = clang_getLocation((void*)p0, (void*)p1, p2, p3);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge30 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jlong p3) {
    *(CXSourceLocation*)p3 = clang_getLocationForOffset((void*)p0, (void*)p1, p2);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge31 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Location_isInSystemHeader(*(CXSourceLocation*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge32 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Location_isFromMainFile(*(CXSourceLocation*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge33 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    *(CXSourceRange*)p0 = clang_getNullRange();
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge34 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    *(CXSourceRange*)p2 = clang_getRange(*(CXSourceLocation*)p0, *(CXSourceLocation*)p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge35 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_equalRanges(*(CXSourceRange*)p0, *(CXSourceRange*)p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge36 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Range_isNull(*(CXSourceRange*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge37 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4) {
    clang_getExpansionLocation(*(CXSourceLocation*)p0, (void*)p1, (void*)p2, (void*)p3, (void*)p4);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge38 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    clang_getPresumedLocation(*(CXSourceLocation*)p0, (void*)p1, (void*)p2, (void*)p3);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge39 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4) {
    clang_getInstantiationLocation(*(CXSourceLocation*)p0, (void*)p1, (void*)p2, (void*)p3, (void*)p4);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge40 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4) {
    clang_getSpellingLocation(*(CXSourceLocation*)p0, (void*)p1, (void*)p2, (void*)p3, (void*)p4);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge41 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4) {
    clang_getFileLocation(*(CXSourceLocation*)p0, (void*)p1, (void*)p2, (void*)p3, (void*)p4);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge42 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXSourceLocation*)p1 = clang_getRangeStart(*(CXSourceRange*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge43 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXSourceLocation*)p1 = clang_getRangeEnd(*(CXSourceRange*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge44 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)clang_getSkippedRanges((void*)p0, (void*)p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge45 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_getAllSkippedRanges((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge46 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_disposeSourceRangeList((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge47 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getNumDiagnosticsInSet((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge48 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)clang_getDiagnosticInSet((void*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge49 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jlong)clang_loadDiagnostics((void*)p0, (void*)p1, (void*)p2);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge50 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_disposeDiagnosticSet((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge51 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_getChildDiagnostics((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge52 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getNumDiagnostics((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge53 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)clang_getDiagnostic((void*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge54 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_getDiagnosticSetFromTU((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge55 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_disposeDiagnostic((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge56 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    *(CXString*)p2 = clang_formatDiagnostic((void*)p0, p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge57 (JNIEnv* jniEnv, jclass jclss) {
    return (jint)clang_defaultDiagnosticDisplayOptions();
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge58 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getDiagnosticSeverity((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge59 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXSourceLocation*)p1 = clang_getDiagnosticLocation((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge60 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_getDiagnosticSpelling((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge61 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    *(CXString*)p2 = clang_getDiagnosticOption((void*)p0, (void*)p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge62 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getDiagnosticCategory((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge63 (JNIEnv* jniEnv, jclass jclss, jint p0, jlong p1) {
    *(CXString*)p1 = clang_getDiagnosticCategoryName(p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge64 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_getDiagnosticCategoryText((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge65 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getDiagnosticNumRanges((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge66 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    *(CXSourceRange*)p2 = clang_getDiagnosticRange((void*)p0, p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge67 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getDiagnosticNumFixIts((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge68 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3) {
    *(CXString*)p3 = clang_getDiagnosticFixIt((void*)p0, p1, (void*)p2);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge69 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_getTranslationUnitSpelling((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge70 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jlong p3, jint p4, jlong p5) {
    return (jlong)clang_createTranslationUnitFromSourceFile((void*)p0, (void*)p1, p2, (void*)p3, p4, (void*)p5);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge71 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)clang_createTranslationUnit((void*)p0, (void*)p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge72 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jint)clang_createTranslationUnit2((void*)p0, (void*)p1, (void*)p2);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge73 (JNIEnv* jniEnv, jclass jclss) {
    return (jint)clang_defaultEditingTranslationUnitOptions();
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge74 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jlong p4, jint p5, jint p6) {
    return (jlong)clang_parseTranslationUnit((void*)p0, (void*)p1, (void*)p2, p3, (void*)p4, p5, p6);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge75 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jlong p4, jint p5, jint p6, jlong p7) {
    return (jint)clang_parseTranslationUnit2((void*)p0, (void*)p1, (void*)p2, p3, (void*)p4, p5, p6, (void*)p7);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge76 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jlong p4, jint p5, jint p6, jlong p7) {
    return (jint)clang_parseTranslationUnit2FullArgv((void*)p0, (void*)p1, (void*)p2, p3, (void*)p4, p5, p6, (void*)p7);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge77 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_defaultSaveOptions((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge78 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jint)clang_saveTranslationUnit((void*)p0, (void*)p1, p2);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge79 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_suspendTranslationUnit((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge80 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_disposeTranslationUnit((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge81 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_defaultReparseOptions((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge82 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jint p3) {
    return (jint)clang_reparseTranslationUnit((void*)p0, p1, (void*)p2, p3);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge83 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jlong)clang_getTUResourceUsageName(p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge84 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(struct CXTUResourceUsage*)p1 = clang_getCXTUResourceUsage((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge85 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_disposeCXTUResourceUsage(*(struct CXTUResourceUsage*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge86 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_getTranslationUnitTargetInfo((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge87 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_TargetInfo_dispose((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge88 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_TargetInfo_getTriple((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge89 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_TargetInfo_getPointerWidth((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge90 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    *(CXCursor*)p0 = clang_getNullCursor();
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge91 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXCursor*)p1 = clang_getTranslationUnitCursor((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge92 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_equalCursors(*(CXCursor*)p0, *(CXCursor*)p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge93 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_isNull(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge94 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_hashCursor(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge95 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getCursorKind(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge96 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jint)clang_isDeclaration(p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge97 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jint)clang_isReference(p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge98 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jint)clang_isExpression(p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge99 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jint)clang_isStatement(p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge100 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jint)clang_isAttribute(p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge101 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_hasAttrs(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge102 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jint)clang_isInvalid(p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge103 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jint)clang_isTranslationUnit(p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge104 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jint)clang_isPreprocessing(p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge105 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jint)clang_isUnexposed(p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge106 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getCursorLinkage(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge107 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getCursorVisibility(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge108 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getCursorAvailability(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge109 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jlong p5, jint p6) {
    return (jint)clang_getCursorPlatformAvailability(*(CXCursor*)p0, (void*)p1, (void*)p2, (void*)p3, (void*)p4, (void*)p5, p6);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge110 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_disposeCXPlatformAvailability((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge111 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getCursorLanguage(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge112 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getCursorTLSKind(*(CXCursor*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge113 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_Cursor_getTranslationUnit(*(CXCursor*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge114 (JNIEnv* jniEnv, jclass jclss) {
    return (jlong)clang_createCXCursorSet();
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge115 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_disposeCXCursorSet((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge116 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_CXCursorSet_contains((void*)p0, *(CXCursor*)p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge117 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_CXCursorSet_insert((void*)p0, *(CXCursor*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge118 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXCursor*)p1 = clang_getCursorSemanticParent(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge119 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXCursor*)p1 = clang_getCursorLexicalParent(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge120 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    clang_getOverriddenCursors(*(CXCursor*)p0, (void*)p1, (void*)p2);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge121 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_disposeOverriddenCursors((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge122 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_getIncludedFile(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge123 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    *(CXCursor*)p2 = clang_getCursor((void*)p0, *(CXSourceLocation*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge124 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXSourceLocation*)p1 = clang_getCursorLocation(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge125 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXSourceRange*)p1 = clang_getCursorExtent(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge126 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXType*)p1 = clang_getCursorType(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge127 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_getTypeSpelling(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge128 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXType*)p1 = clang_getTypedefDeclUnderlyingType(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge129 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXType*)p1 = clang_getEnumDeclIntegerType(*(CXCursor*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge130 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_getEnumConstantDeclValue(*(CXCursor*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge131 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_getEnumConstantDeclUnsignedValue(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge132 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getFieldDeclBitWidth(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge133 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_getNumArguments(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge134 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    *(CXCursor*)p2 = clang_Cursor_getArgument(*(CXCursor*)p0, p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge135 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_getNumTemplateArguments(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge136 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jint)clang_Cursor_getTemplateArgumentKind(*(CXCursor*)p0, p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge137 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    *(CXType*)p2 = clang_Cursor_getTemplateArgumentType(*(CXCursor*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge138 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)clang_Cursor_getTemplateArgumentValue(*(CXCursor*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge139 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)clang_Cursor_getTemplateArgumentUnsignedValue(*(CXCursor*)p0, p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge140 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_equalTypes(*(CXType*)p0, *(CXType*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge141 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXType*)p1 = clang_getCanonicalType(*(CXType*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge142 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_isConstQualifiedType(*(CXType*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge143 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_isMacroFunctionLike(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge144 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_isMacroBuiltin(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge145 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_isFunctionInlined(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge146 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_isVolatileQualifiedType(*(CXType*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge147 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_isRestrictQualifiedType(*(CXType*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge148 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getAddressSpace(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge149 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_getTypedefName(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge150 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXType*)p1 = clang_getPointeeType(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge151 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXCursor*)p1 = clang_getTypeDeclaration(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge152 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_getDeclObjCTypeEncoding(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge153 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_Type_getObjCEncoding(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge154 (JNIEnv* jniEnv, jclass jclss, jint p0, jlong p1) {
    *(CXString*)p1 = clang_getTypeKindSpelling(p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge155 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getFunctionTypeCallingConv(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge156 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXType*)p1 = clang_getResultType(*(CXType*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge157 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getExceptionSpecificationType(*(CXType*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge158 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getNumArgTypes(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge159 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    *(CXType*)p2 = clang_getArgType(*(CXType*)p0, p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge160 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_isFunctionTypeVariadic(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge161 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXType*)p1 = clang_getCursorResultType(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge162 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getCursorExceptionSpecificationType(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge163 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_isPODType(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge164 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXType*)p1 = clang_getElementType(*(CXType*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge165 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_getNumElements(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge166 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXType*)p1 = clang_getArrayElementType(*(CXType*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge167 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_getArraySize(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge168 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXType*)p1 = clang_Type_getNamedType(*(CXType*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge169 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Type_isTransparentTagTypedef(*(CXType*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge170 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_Type_getAlignOf(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge171 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXType*)p1 = clang_Type_getClassType(*(CXType*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge172 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_Type_getSizeOf(*(CXType*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge173 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)clang_Type_getOffsetOf(*(CXType*)p0, (void*)p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge174 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_Cursor_getOffsetOfField(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge175 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_isAnonymous(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge176 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Type_getNumTemplateArguments(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge177 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    *(CXType*)p2 = clang_Type_getTemplateArgumentAsType(*(CXType*)p0, p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge178 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Type_getCXXRefQualifier(*(CXType*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge179 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_isBitField(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge180 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_isVirtualBase(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge181 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getCXXAccessSpecifier(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge182 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_getStorageClass(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge183 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getNumOverloadedDecls(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge184 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    *(CXCursor*)p2 = clang_getOverloadedDecl(*(CXCursor*)p0, p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge185 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXType*)p1 = clang_getIBOutletCollectionType(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge186 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jint)clang_visitChildren(*(CXCursor*)p0, (void*)p1, (void*)p2);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge187 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_getCursorUSR(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge188 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_constructUSR_ObjCClass((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge189 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    *(CXString*)p2 = clang_constructUSR_ObjCCategory((void*)p0, (void*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge190 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_constructUSR_ObjCProtocol((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge191 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    *(CXString*)p2 = clang_constructUSR_ObjCIvar((void*)p0, *(CXString*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge192 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3) {
    *(CXString*)p3 = clang_constructUSR_ObjCMethod((void*)p0, p1, *(CXString*)p2);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge193 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    *(CXString*)p2 = clang_constructUSR_ObjCProperty((void*)p0, *(CXString*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge194 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_getCursorSpelling(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge195 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jint p2, jlong p3) {
    *(CXSourceRange*)p3 = clang_Cursor_getSpellingNameRange(*(CXCursor*)p0, p1, p2);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge196 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_getCursorDisplayName(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge197 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXCursor*)p1 = clang_getCursorReferenced(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge198 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXCursor*)p1 = clang_getCursorDefinition(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge199 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_isCursorDefinition(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge200 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXCursor*)p1 = clang_getCanonicalCursor(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge201 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_getObjCSelectorIndex(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge202 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_isDynamicCall(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge203 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXType*)p1 = clang_Cursor_getReceiverType(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge204 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jint)clang_Cursor_getObjCPropertyAttributes(*(CXCursor*)p0, p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge205 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_getObjCDeclQualifiers(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge206 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_isObjCOptional(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge207 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_isVariadic(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge208 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    return (jint)clang_Cursor_isExternalSymbol(*(CXCursor*)p0, (void*)p1, (void*)p2, (void*)p3);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge209 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXSourceRange*)p1 = clang_Cursor_getCommentRange(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge210 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_Cursor_getRawCommentText(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge211 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_Cursor_getBriefCommentText(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge212 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_Cursor_getMangling(*(CXCursor*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge213 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_Cursor_getCXXManglings(*(CXCursor*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge214 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_Cursor_getObjCManglings(*(CXCursor*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge215 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_Cursor_getModule(*(CXCursor*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge216 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jlong)clang_getModuleForFile((void*)p0, (void*)p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge217 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_Module_getASTFile((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge218 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_Module_getParent((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge219 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_Module_getName((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge220 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_Module_getFullName((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge221 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Module_isSystem((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge222 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_Module_getNumTopLevelHeaders((void*)p0, (void*)p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge223 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    return (jlong)clang_Module_getTopLevelHeader((void*)p0, (void*)p1, p2);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge224 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_CXXConstructor_isConvertingConstructor(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge225 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_CXXConstructor_isCopyConstructor(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge226 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_CXXConstructor_isDefaultConstructor(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge227 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_CXXConstructor_isMoveConstructor(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge228 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_CXXField_isMutable(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge229 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_CXXMethod_isDefaulted(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge230 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_CXXMethod_isPureVirtual(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge231 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_CXXMethod_isStatic(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge232 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_CXXMethod_isVirtual(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge233 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_CXXRecord_isAbstract(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge234 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_EnumDecl_isScoped(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge235 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_CXXMethod_isConst(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge236 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getTemplateCursorKind(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge237 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXCursor*)p1 = clang_getSpecializedCursorTemplate(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge238 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jint p2, jlong p3) {
    *(CXSourceRange*)p3 = clang_getCursorReferenceNameRange(*(CXCursor*)p0, p1, p2);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge239 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getTokenKind(*(CXToken*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge240 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    *(CXString*)p2 = clang_getTokenSpelling((void*)p0, *(CXToken*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge241 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    *(CXSourceLocation*)p2 = clang_getTokenLocation((void*)p0, *(CXToken*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge242 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    *(CXSourceRange*)p2 = clang_getTokenExtent((void*)p0, *(CXToken*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge243 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3) {
    clang_tokenize((void*)p0, *(CXSourceRange*)p1, (void*)p2, (void*)p3);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge244 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jlong p3) {
    clang_annotateTokens((void*)p0, (void*)p1, p2, (void*)p3);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge245 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    clang_disposeTokens((void*)p0, (void*)p1, p2);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge246 (JNIEnv* jniEnv, jclass jclss, jint p0, jlong p1) {
    *(CXString*)p1 = clang_getCursorKindSpelling(p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge247 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jlong p5, jlong p6) {
    clang_getDefinitionSpellingAndExtent(*(CXCursor*)p0, (void*)p1, (void*)p2, (void*)p3, (void*)p4, (void*)p5, (void*)p6);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge248 (JNIEnv* jniEnv, jclass jclss) {
    clang_enableStackTraces();
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge249 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2) {
    clang_executeOnThread((void*)p0, (void*)p1, p2);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge250 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jint)clang_getCompletionChunkKind((void*)p0, p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge251 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    *(CXString*)p2 = clang_getCompletionChunkText((void*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge252 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)clang_getCompletionChunkCompletionString((void*)p0, p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge253 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getNumCompletionChunks((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge254 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getCompletionPriority((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge255 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getCompletionAvailability((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge256 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_getCompletionNumAnnotations((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge257 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    *(CXString*)p2 = clang_getCompletionAnnotation((void*)p0, p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge258 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    *(CXString*)p2 = clang_getCompletionParent((void*)p0, (void*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge259 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_getCompletionBriefComment((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge260 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_getCursorCompletionString(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge261 (JNIEnv* jniEnv, jclass jclss) {
    return (jint)clang_defaultCodeCompleteOptions();
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge262 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jint p2, jint p3, jlong p4, jint p5, jint p6) {
    return (jlong)clang_codeCompleteAt((void*)p0, (void*)p1, p2, p3, (void*)p4, p5, p6);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge263 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    clang_sortCodeCompletionResults((void*)p0, p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge264 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_disposeCodeCompleteResults((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge265 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_codeCompleteGetNumDiagnostics((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge266 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)clang_codeCompleteGetDiagnostic((void*)p0, p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge267 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_codeCompleteGetContexts((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge268 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_codeCompleteGetContainerKind((void*)p0, (void*)p1);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge269 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_codeCompleteGetContainerUSR((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge270 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXString*)p1 = clang_codeCompleteGetObjCSelector((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge271 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    *(CXString*)p0 = clang_getClangVersion();
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge272 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    clang_toggleCrashRecovery(p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge273 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    clang_getInclusions((void*)p0, (void*)p1, (void*)p2);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge274 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_Cursor_Evaluate(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge275 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_EvalResult_getKind((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge276 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_EvalResult_getAsInt((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge277 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_EvalResult_getAsLongLong((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge278 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_EvalResult_isUnsignedInt((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge279 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_EvalResult_getAsUnsigned((void*)p0);
}
JNIEXPORT jdouble JNICALL Java_clang_clang_kniBridge280 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jdouble)clang_EvalResult_getAsDouble((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge281 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_EvalResult_getAsStr((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge282 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_EvalResult_dispose((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge283 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_getRemappings((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge284 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1) {
    return (jlong)clang_getRemappingsFromFileList((void*)p0, p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge285 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_remap_getNumFiles((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge286 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2, jlong p3) {
    clang_remap_getFilenames((void*)p0, p1, (void*)p2, (void*)p3);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge287 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_remap_dispose((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge288 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jint)clang_findReferencesInFile(*(CXCursor*)p0, (void*)p1, *(struct CXCursorAndRangeVisitor*)p2);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge289 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jint)clang_findIncludesInFile((void*)p0, (void*)p1, *(struct CXCursorAndRangeVisitor*)p2);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge290 (JNIEnv* jniEnv, jclass jclss, jint p0) {
    return (jint)clang_index_isEntityObjCContainerKind(p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge291 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_index_getObjCContainerDeclInfo((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge292 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_index_getObjCInterfaceDeclInfo((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge293 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_index_getObjCCategoryDeclInfo((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge294 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_index_getObjCProtocolRefListInfo((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge295 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_index_getObjCPropertyDeclInfo((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge296 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_index_getIBOutletCollectionAttrInfo((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge297 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_index_getCXXClassDeclInfo((void*)p0);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge298 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_index_getClientContainer((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge299 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    clang_index_setClientContainer((void*)p0, (void*)p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge300 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_index_getClientEntity((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge301 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    clang_index_setClientEntity((void*)p0, (void*)p1);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge302 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_IndexAction_create((void*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge303 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    clang_IndexAction_dispose((void*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge304 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jint p4, jlong p5, jlong p6, jint p7, jlong p8, jint p9, jlong p10, jint p11) {
    return (jint)clang_indexSourceFile((void*)p0, (void*)p1, (void*)p2, p3, p4, (void*)p5, (void*)p6, p7, (void*)p8, p9, (void*)p10, p11);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge305 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jint p4, jlong p5, jlong p6, jint p7, jlong p8, jint p9, jlong p10, jint p11) {
    return (jint)clang_indexSourceFileFullArgv((void*)p0, (void*)p1, (void*)p2, p3, p4, (void*)p5, (void*)p6, p7, (void*)p8, p9, (void*)p10, p11);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge306 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jint p3, jint p4, jlong p5) {
    return (jint)clang_indexTranslationUnit((void*)p0, (void*)p1, (void*)p2, p3, p4, (void*)p5);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge307 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2, jlong p3, jlong p4, jlong p5) {
    clang_indexLoc_getFileLocation(*(CXIdxLoc*)p0, (void*)p1, (void*)p2, (void*)p3, (void*)p4, (void*)p5);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge308 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXSourceLocation*)p1 = clang_indexLoc_getCXSourceLocation(*(CXIdxLoc*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge309 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1, jlong p2) {
    return (jint)clang_Type_visitFields(*(CXType*)p0, (void*)p1, (void*)p2);
}
JNIEXPORT jlong JNICALL Java_clang_clang_kniBridge310 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jlong)clang_Cursor_getAttributeSpelling(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge311 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXTypeAttributes*)p1 = clang_getDeclTypeAttributes(*(CXCursor*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge312 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXTypeAttributes*)p1 = clang_getResultTypeAttributes(*(CXTypeAttributes*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge313 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    *(CXTypeAttributes*)p1 = clang_getCursorResultTypeAttributes(*(CXCursor*)p0);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge314 (JNIEnv* jniEnv, jclass jclss, jlong p0, jlong p1) {
    return (jint)clang_Type_getNullabilityKind(*(CXType*)p0, *(CXTypeAttributes*)p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge315 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Type_getNumProtocols(*(CXType*)p0);
}
JNIEXPORT void JNICALL Java_clang_clang_kniBridge316 (JNIEnv* jniEnv, jclass jclss, jlong p0, jint p1, jlong p2) {
    *(CXCursor*)p2 = clang_Type_getProtocol(*(CXType*)p0, p1);
}
JNIEXPORT jint JNICALL Java_clang_clang_kniBridge317 (JNIEnv* jniEnv, jclass jclss, jlong p0) {
    return (jint)clang_Cursor_isObjCInitMethod(*(CXCursor*)p0);
}
