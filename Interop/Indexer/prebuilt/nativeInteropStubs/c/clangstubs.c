#include <stdint.h>
#include <jni.h>
#include <clang-c/Index.h>

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1asctime (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (asctime((struct tm*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clock (JNIEnv *jniEnv, jclass jclss) {
    return (jlong) (clock());
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1ctime (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (ctime((time_t*)arg0));
}

JNIEXPORT jdouble JNICALL Java_clang_clang_kni_1difftime (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1) {
    return (jdouble) (difftime((time_t)arg0, (time_t)arg1));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1getdate (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (getdate((char*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1gmtime (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (gmtime((time_t*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1localtime (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (localtime((time_t*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1mktime (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (mktime((struct tm*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1strftime (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1, jlong arg2, jlong arg3) {
    return (jlong) (strftime((char*)arg0, (size_t)arg1, (char*)arg2, (struct tm*)arg3));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1strptime (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1, jlong arg2) {
    return (jlong) (strptime((char*)arg0, (char*)arg1, (struct tm*)arg2));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1time (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (time((time_t*)arg0));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1tzset (JNIEnv *jniEnv, jclass jclss) {
    tzset();
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1asctime_1r (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1) {
    return (jlong) (asctime_r((struct tm*)arg0, (char*)arg1));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1ctime_1r (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1) {
    return (jlong) (ctime_r((time_t*)arg0, (char*)arg1));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1gmtime_1r (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1) {
    return (jlong) (gmtime_r((time_t*)arg0, (struct tm*)arg1));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1localtime_1r (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1) {
    return (jlong) (localtime_r((time_t*)arg0, (struct tm*)arg1));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1posix2time (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (posix2time((time_t)arg0));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1tzsetwall (JNIEnv *jniEnv, jclass jclss) {
    tzsetwall();
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1time2posix (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (time2posix((time_t)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1timelocal (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (timelocal((struct tm*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1timegm (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (timegm((struct tm*)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1nanosleep (JNIEnv *jniEnv, jclass jclss, jlong __rqtp, jlong __rmtp) {
    return (jint) (nanosleep((struct timespec*)__rqtp, (struct timespec*)__rmtp));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clock_1getres (JNIEnv *jniEnv, jclass jclss, jint __clock_id, jlong __res) {
    return (jint) (clock_getres((clockid_t)__clock_id, (struct timespec*)__res));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clock_1gettime (JNIEnv *jniEnv, jclass jclss, jint __clock_id, jlong __tp) {
    return (jint) (clock_gettime((clockid_t)__clock_id, (struct timespec*)__tp));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clock_1gettime_1nsec_1np (JNIEnv *jniEnv, jclass jclss, jint __clock_id) {
    return (jlong) (clock_gettime_nsec_np((clockid_t)__clock_id));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clock_1settime (JNIEnv *jniEnv, jclass jclss, jint __clock_id, jlong __tp) {
    return (jint) (clock_settime((clockid_t)__clock_id, (struct timespec*)__tp));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCString (JNIEnv *jniEnv, jclass jclss, jlong string) {
    return (jlong) (clang_getCString(*(CXString*)string));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1disposeString (JNIEnv *jniEnv, jclass jclss, jlong string) {
    clang_disposeString(*(CXString*)string);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1disposeStringSet (JNIEnv *jniEnv, jclass jclss, jlong set) {
    clang_disposeStringSet((CXStringSet*)set);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getBuildSessionTimestamp (JNIEnv *jniEnv, jclass jclss) {
    return (jlong) (clang_getBuildSessionTimestamp());
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1VirtualFileOverlay_1create (JNIEnv *jniEnv, jclass jclss, jint options) {
    return (jlong) (clang_VirtualFileOverlay_create((unsigned int)options));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1VirtualFileOverlay_1addFileMapping (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong virtualPath, jlong realPath) {
    return (jint) (clang_VirtualFileOverlay_addFileMapping((CXVirtualFileOverlay)arg0, (char*)virtualPath, (char*)realPath));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1VirtualFileOverlay_1setCaseSensitivity (JNIEnv *jniEnv, jclass jclss, jlong arg0, jint caseSensitive) {
    return (jint) (clang_VirtualFileOverlay_setCaseSensitivity((CXVirtualFileOverlay)arg0, (int)caseSensitive));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1VirtualFileOverlay_1writeToBuffer (JNIEnv *jniEnv, jclass jclss, jlong arg0, jint options, jlong out_buffer_ptr, jlong out_buffer_size) {
    return (jint) (clang_VirtualFileOverlay_writeToBuffer((CXVirtualFileOverlay)arg0, (unsigned int)options, (char**)out_buffer_ptr, (unsigned int*)out_buffer_size));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1free (JNIEnv *jniEnv, jclass jclss, jlong buffer) {
    clang_free((void*)buffer);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1VirtualFileOverlay_1dispose (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    clang_VirtualFileOverlay_dispose((CXVirtualFileOverlay)arg0);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1ModuleMapDescriptor_1create (JNIEnv *jniEnv, jclass jclss, jint options) {
    return (jlong) (clang_ModuleMapDescriptor_create((unsigned int)options));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1ModuleMapDescriptor_1setFrameworkModuleName (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong name) {
    return (jint) (clang_ModuleMapDescriptor_setFrameworkModuleName((CXModuleMapDescriptor)arg0, (char*)name));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1ModuleMapDescriptor_1setUmbrellaHeader (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong name) {
    return (jint) (clang_ModuleMapDescriptor_setUmbrellaHeader((CXModuleMapDescriptor)arg0, (char*)name));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1ModuleMapDescriptor_1writeToBuffer (JNIEnv *jniEnv, jclass jclss, jlong arg0, jint options, jlong out_buffer_ptr, jlong out_buffer_size) {
    return (jint) (clang_ModuleMapDescriptor_writeToBuffer((CXModuleMapDescriptor)arg0, (unsigned int)options, (char**)out_buffer_ptr, (unsigned int*)out_buffer_size));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1ModuleMapDescriptor_1dispose (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    clang_ModuleMapDescriptor_dispose((CXModuleMapDescriptor)arg0);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1createIndex (JNIEnv *jniEnv, jclass jclss, jint excludeDeclarationsFromPCH, jint displayDiagnostics) {
    return (jlong) (clang_createIndex((int)excludeDeclarationsFromPCH, (int)displayDiagnostics));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1disposeIndex (JNIEnv *jniEnv, jclass jclss, jlong index) {
    clang_disposeIndex((CXIndex)index);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1CXIndex_1setGlobalOptions (JNIEnv *jniEnv, jclass jclss, jlong arg0, jint options) {
    clang_CXIndex_setGlobalOptions((CXIndex)arg0, (unsigned int)options);
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1CXIndex_1getGlobalOptions (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jint) (clang_CXIndex_getGlobalOptions((CXIndex)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getFileName (JNIEnv *jniEnv, jclass jclss, jlong SFile, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getFileName((CXFile)SFile);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getFileTime (JNIEnv *jniEnv, jclass jclss, jlong SFile) {
    return (jlong) (clang_getFileTime((CXFile)SFile));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getFileUniqueID (JNIEnv *jniEnv, jclass jclss, jlong file, jlong outID) {
    return (jint) (clang_getFileUniqueID((CXFile)file, (CXFileUniqueID*)outID));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isFileMultipleIncludeGuarded (JNIEnv *jniEnv, jclass jclss, jlong tu, jlong file) {
    return (jint) (clang_isFileMultipleIncludeGuarded((CXTranslationUnit)tu, (CXFile)file));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getFile (JNIEnv *jniEnv, jclass jclss, jlong tu, jlong file_name) {
    return (jlong) (clang_getFile((CXTranslationUnit)tu, (char*)file_name));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1File_1isEqual (JNIEnv *jniEnv, jclass jclss, jlong file1, jlong file2) {
    return (jint) (clang_File_isEqual((CXFile)file1, (CXFile)file2));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getNullLocation (JNIEnv *jniEnv, jclass jclss, jlong retValPlacement) {
    *(CXSourceLocation*)retValPlacement = clang_getNullLocation();
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1equalLocations (JNIEnv *jniEnv, jclass jclss, jlong loc1, jlong loc2) {
    return (jint) (clang_equalLocations(*(CXSourceLocation*)loc1, *(CXSourceLocation*)loc2));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getLocation (JNIEnv *jniEnv, jclass jclss, jlong tu, jlong file, jint line, jint column, jlong retValPlacement) {
    *(CXSourceLocation*)retValPlacement = clang_getLocation((CXTranslationUnit)tu, (CXFile)file, (unsigned int)line, (unsigned int)column);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getLocationForOffset (JNIEnv *jniEnv, jclass jclss, jlong tu, jlong file, jint offset, jlong retValPlacement) {
    *(CXSourceLocation*)retValPlacement = clang_getLocationForOffset((CXTranslationUnit)tu, (CXFile)file, (unsigned int)offset);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Location_1isInSystemHeader (JNIEnv *jniEnv, jclass jclss, jlong location) {
    return (jint) (clang_Location_isInSystemHeader(*(CXSourceLocation*)location));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Location_1isFromMainFile (JNIEnv *jniEnv, jclass jclss, jlong location) {
    return (jint) (clang_Location_isFromMainFile(*(CXSourceLocation*)location));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getNullRange (JNIEnv *jniEnv, jclass jclss, jlong retValPlacement) {
    *(CXSourceRange*)retValPlacement = clang_getNullRange();
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getRange (JNIEnv *jniEnv, jclass jclss, jlong begin, jlong end, jlong retValPlacement) {
    *(CXSourceRange*)retValPlacement = clang_getRange(*(CXSourceLocation*)begin, *(CXSourceLocation*)end);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1equalRanges (JNIEnv *jniEnv, jclass jclss, jlong range1, jlong range2) {
    return (jint) (clang_equalRanges(*(CXSourceRange*)range1, *(CXSourceRange*)range2));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Range_1isNull (JNIEnv *jniEnv, jclass jclss, jlong range) {
    return (jint) (clang_Range_isNull(*(CXSourceRange*)range));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1getExpansionLocation (JNIEnv *jniEnv, jclass jclss, jlong location, jlong file, jlong line, jlong column, jlong offset) {
    clang_getExpansionLocation(*(CXSourceLocation*)location, (CXFile*)file, (unsigned int*)line, (unsigned int*)column, (unsigned int*)offset);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1getPresumedLocation (JNIEnv *jniEnv, jclass jclss, jlong location, jlong filename, jlong line, jlong column) {
    clang_getPresumedLocation(*(CXSourceLocation*)location, (CXString*)filename, (unsigned int*)line, (unsigned int*)column);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1getInstantiationLocation (JNIEnv *jniEnv, jclass jclss, jlong location, jlong file, jlong line, jlong column, jlong offset) {
    clang_getInstantiationLocation(*(CXSourceLocation*)location, (CXFile*)file, (unsigned int*)line, (unsigned int*)column, (unsigned int*)offset);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1getSpellingLocation (JNIEnv *jniEnv, jclass jclss, jlong location, jlong file, jlong line, jlong column, jlong offset) {
    clang_getSpellingLocation(*(CXSourceLocation*)location, (CXFile*)file, (unsigned int*)line, (unsigned int*)column, (unsigned int*)offset);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1getFileLocation (JNIEnv *jniEnv, jclass jclss, jlong location, jlong file, jlong line, jlong column, jlong offset) {
    clang_getFileLocation(*(CXSourceLocation*)location, (CXFile*)file, (unsigned int*)line, (unsigned int*)column, (unsigned int*)offset);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getRangeStart (JNIEnv *jniEnv, jclass jclss, jlong range, jlong retValPlacement) {
    *(CXSourceLocation*)retValPlacement = clang_getRangeStart(*(CXSourceRange*)range);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getRangeEnd (JNIEnv *jniEnv, jclass jclss, jlong range, jlong retValPlacement) {
    *(CXSourceLocation*)retValPlacement = clang_getRangeEnd(*(CXSourceRange*)range);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getSkippedRanges (JNIEnv *jniEnv, jclass jclss, jlong tu, jlong file) {
    return (jlong) (clang_getSkippedRanges((CXTranslationUnit)tu, (CXFile)file));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1disposeSourceRangeList (JNIEnv *jniEnv, jclass jclss, jlong ranges) {
    clang_disposeSourceRangeList((CXSourceRangeList*)ranges);
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getNumDiagnosticsInSet (JNIEnv *jniEnv, jclass jclss, jlong Diags) {
    return (jint) (clang_getNumDiagnosticsInSet((CXDiagnosticSet)Diags));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getDiagnosticInSet (JNIEnv *jniEnv, jclass jclss, jlong Diags, jint Index) {
    return (jlong) (clang_getDiagnosticInSet((CXDiagnosticSet)Diags, (unsigned int)Index));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1loadDiagnostics (JNIEnv *jniEnv, jclass jclss, jlong file, jlong error, jlong errorString) {
    return (jlong) (clang_loadDiagnostics((char*)file, (enum CXLoadDiag_Error*)error, (CXString*)errorString));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1disposeDiagnosticSet (JNIEnv *jniEnv, jclass jclss, jlong Diags) {
    clang_disposeDiagnosticSet((CXDiagnosticSet)Diags);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getChildDiagnostics (JNIEnv *jniEnv, jclass jclss, jlong D) {
    return (jlong) (clang_getChildDiagnostics((CXDiagnostic)D));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getNumDiagnostics (JNIEnv *jniEnv, jclass jclss, jlong Unit) {
    return (jint) (clang_getNumDiagnostics((CXTranslationUnit)Unit));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getDiagnostic (JNIEnv *jniEnv, jclass jclss, jlong Unit, jint Index) {
    return (jlong) (clang_getDiagnostic((CXTranslationUnit)Unit, (unsigned int)Index));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getDiagnosticSetFromTU (JNIEnv *jniEnv, jclass jclss, jlong Unit) {
    return (jlong) (clang_getDiagnosticSetFromTU((CXTranslationUnit)Unit));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1disposeDiagnostic (JNIEnv *jniEnv, jclass jclss, jlong Diagnostic) {
    clang_disposeDiagnostic((CXDiagnostic)Diagnostic);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1formatDiagnostic (JNIEnv *jniEnv, jclass jclss, jlong Diagnostic, jint Options, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_formatDiagnostic((CXDiagnostic)Diagnostic, (unsigned int)Options);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1defaultDiagnosticDisplayOptions (JNIEnv *jniEnv, jclass jclss) {
    return (jint) (clang_defaultDiagnosticDisplayOptions());
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getDiagnosticSeverity (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jint) (clang_getDiagnosticSeverity((CXDiagnostic)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getDiagnosticLocation (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXSourceLocation*)retValPlacement = clang_getDiagnosticLocation((CXDiagnostic)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getDiagnosticSpelling (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getDiagnosticSpelling((CXDiagnostic)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getDiagnosticOption (JNIEnv *jniEnv, jclass jclss, jlong Diag, jlong Disable, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getDiagnosticOption((CXDiagnostic)Diag, (CXString*)Disable);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getDiagnosticCategory (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jint) (clang_getDiagnosticCategory((CXDiagnostic)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getDiagnosticCategoryName (JNIEnv *jniEnv, jclass jclss, jint Category, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getDiagnosticCategoryName((unsigned int)Category);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getDiagnosticCategoryText (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getDiagnosticCategoryText((CXDiagnostic)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getDiagnosticNumRanges (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jint) (clang_getDiagnosticNumRanges((CXDiagnostic)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getDiagnosticRange (JNIEnv *jniEnv, jclass jclss, jlong Diagnostic, jint Range, jlong retValPlacement) {
    *(CXSourceRange*)retValPlacement = clang_getDiagnosticRange((CXDiagnostic)Diagnostic, (unsigned int)Range);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getDiagnosticNumFixIts (JNIEnv *jniEnv, jclass jclss, jlong Diagnostic) {
    return (jint) (clang_getDiagnosticNumFixIts((CXDiagnostic)Diagnostic));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getDiagnosticFixIt (JNIEnv *jniEnv, jclass jclss, jlong Diagnostic, jint FixIt, jlong ReplacementRange, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getDiagnosticFixIt((CXDiagnostic)Diagnostic, (unsigned int)FixIt, (CXSourceRange*)ReplacementRange);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getTranslationUnitSpelling (JNIEnv *jniEnv, jclass jclss, jlong CTUnit, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getTranslationUnitSpelling((CXTranslationUnit)CTUnit);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1createTranslationUnitFromSourceFile (JNIEnv *jniEnv, jclass jclss, jlong CIdx, jlong source_filename, jint num_clang_command_line_args, jlong clang_command_line_args, jint num_unsaved_files, jlong unsaved_files) {
    return (jlong) (clang_createTranslationUnitFromSourceFile((CXIndex)CIdx, (char*)source_filename, (int)num_clang_command_line_args, (char**)clang_command_line_args, (unsigned int)num_unsaved_files, (struct CXUnsavedFile*)unsaved_files));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1createTranslationUnit (JNIEnv *jniEnv, jclass jclss, jlong CIdx, jlong ast_filename) {
    return (jlong) (clang_createTranslationUnit((CXIndex)CIdx, (char*)ast_filename));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1createTranslationUnit2 (JNIEnv *jniEnv, jclass jclss, jlong CIdx, jlong ast_filename, jlong out_TU) {
    return (jint) (clang_createTranslationUnit2((CXIndex)CIdx, (char*)ast_filename, (CXTranslationUnit*)out_TU));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1defaultEditingTranslationUnitOptions (JNIEnv *jniEnv, jclass jclss) {
    return (jint) (clang_defaultEditingTranslationUnitOptions());
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1parseTranslationUnit (JNIEnv *jniEnv, jclass jclss, jlong CIdx, jlong source_filename, jlong command_line_args, jint num_command_line_args, jlong unsaved_files, jint num_unsaved_files, jint options) {
    return (jlong) (clang_parseTranslationUnit((CXIndex)CIdx, (char*)source_filename, (char**)command_line_args, (int)num_command_line_args, (struct CXUnsavedFile*)unsaved_files, (unsigned int)num_unsaved_files, (unsigned int)options));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1parseTranslationUnit2 (JNIEnv *jniEnv, jclass jclss, jlong CIdx, jlong source_filename, jlong command_line_args, jint num_command_line_args, jlong unsaved_files, jint num_unsaved_files, jint options, jlong out_TU) {
    return (jint) (clang_parseTranslationUnit2((CXIndex)CIdx, (char*)source_filename, (char**)command_line_args, (int)num_command_line_args, (struct CXUnsavedFile*)unsaved_files, (unsigned int)num_unsaved_files, (unsigned int)options, (CXTranslationUnit*)out_TU));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1parseTranslationUnit2FullArgv (JNIEnv *jniEnv, jclass jclss, jlong CIdx, jlong source_filename, jlong command_line_args, jint num_command_line_args, jlong unsaved_files, jint num_unsaved_files, jint options, jlong out_TU) {
    return (jint) (clang_parseTranslationUnit2FullArgv((CXIndex)CIdx, (char*)source_filename, (char**)command_line_args, (int)num_command_line_args, (struct CXUnsavedFile*)unsaved_files, (unsigned int)num_unsaved_files, (unsigned int)options, (CXTranslationUnit*)out_TU));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1defaultSaveOptions (JNIEnv *jniEnv, jclass jclss, jlong TU) {
    return (jint) (clang_defaultSaveOptions((CXTranslationUnit)TU));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1saveTranslationUnit (JNIEnv *jniEnv, jclass jclss, jlong TU, jlong FileName, jint options) {
    return (jint) (clang_saveTranslationUnit((CXTranslationUnit)TU, (char*)FileName, (unsigned int)options));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1disposeTranslationUnit (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    clang_disposeTranslationUnit((CXTranslationUnit)arg0);
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1defaultReparseOptions (JNIEnv *jniEnv, jclass jclss, jlong TU) {
    return (jint) (clang_defaultReparseOptions((CXTranslationUnit)TU));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1reparseTranslationUnit (JNIEnv *jniEnv, jclass jclss, jlong TU, jint num_unsaved_files, jlong unsaved_files, jint options) {
    return (jint) (clang_reparseTranslationUnit((CXTranslationUnit)TU, (unsigned int)num_unsaved_files, (struct CXUnsavedFile*)unsaved_files, (unsigned int)options));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getTUResourceUsageName (JNIEnv *jniEnv, jclass jclss, jint kind) {
    return (jlong) (clang_getTUResourceUsageName((enum CXTUResourceUsageKind)kind));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCXTUResourceUsage (JNIEnv *jniEnv, jclass jclss, jlong TU, jlong retValPlacement) {
    *(struct CXTUResourceUsage*)retValPlacement = clang_getCXTUResourceUsage((CXTranslationUnit)TU);
    return (jlong) retValPlacement;
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1disposeCXTUResourceUsage (JNIEnv *jniEnv, jclass jclss, jlong usage) {
    clang_disposeCXTUResourceUsage(*(struct CXTUResourceUsage*)usage);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getNullCursor (JNIEnv *jniEnv, jclass jclss, jlong retValPlacement) {
    *(CXCursor*)retValPlacement = clang_getNullCursor();
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getTranslationUnitCursor (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXCursor*)retValPlacement = clang_getTranslationUnitCursor((CXTranslationUnit)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1equalCursors (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1) {
    return (jint) (clang_equalCursors(*(CXCursor*)arg0, *(CXCursor*)arg1));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1isNull (JNIEnv *jniEnv, jclass jclss, jlong cursor) {
    return (jint) (clang_Cursor_isNull(*(CXCursor*)cursor));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1hashCursor (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jint) (clang_hashCursor(*(CXCursor*)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getCursorKind (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jint) (clang_getCursorKind(*(CXCursor*)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isDeclaration (JNIEnv *jniEnv, jclass jclss, jint arg0) {
    return (jint) (clang_isDeclaration((enum CXCursorKind)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isReference (JNIEnv *jniEnv, jclass jclss, jint arg0) {
    return (jint) (clang_isReference((enum CXCursorKind)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isExpression (JNIEnv *jniEnv, jclass jclss, jint arg0) {
    return (jint) (clang_isExpression((enum CXCursorKind)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isStatement (JNIEnv *jniEnv, jclass jclss, jint arg0) {
    return (jint) (clang_isStatement((enum CXCursorKind)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isAttribute (JNIEnv *jniEnv, jclass jclss, jint arg0) {
    return (jint) (clang_isAttribute((enum CXCursorKind)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1hasAttrs (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_Cursor_hasAttrs(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isInvalid (JNIEnv *jniEnv, jclass jclss, jint arg0) {
    return (jint) (clang_isInvalid((enum CXCursorKind)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isTranslationUnit (JNIEnv *jniEnv, jclass jclss, jint arg0) {
    return (jint) (clang_isTranslationUnit((enum CXCursorKind)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isPreprocessing (JNIEnv *jniEnv, jclass jclss, jint arg0) {
    return (jint) (clang_isPreprocessing((enum CXCursorKind)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isUnexposed (JNIEnv *jniEnv, jclass jclss, jint arg0) {
    return (jint) (clang_isUnexposed((enum CXCursorKind)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getCursorLinkage (JNIEnv *jniEnv, jclass jclss, jlong cursor) {
    return (jint) (clang_getCursorLinkage(*(CXCursor*)cursor));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getCursorVisibility (JNIEnv *jniEnv, jclass jclss, jlong cursor) {
    return (jint) (clang_getCursorVisibility(*(CXCursor*)cursor));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getCursorAvailability (JNIEnv *jniEnv, jclass jclss, jlong cursor) {
    return (jint) (clang_getCursorAvailability(*(CXCursor*)cursor));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getCursorPlatformAvailability (JNIEnv *jniEnv, jclass jclss, jlong cursor, jlong always_deprecated, jlong deprecated_message, jlong always_unavailable, jlong unavailable_message, jlong availability, jint availability_size) {
    return (jint) (clang_getCursorPlatformAvailability(*(CXCursor*)cursor, (int*)always_deprecated, (CXString*)deprecated_message, (int*)always_unavailable, (CXString*)unavailable_message, (struct CXPlatformAvailability*)availability, (int)availability_size));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1disposeCXPlatformAvailability (JNIEnv *jniEnv, jclass jclss, jlong availability) {
    clang_disposeCXPlatformAvailability((struct CXPlatformAvailability*)availability);
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getCursorLanguage (JNIEnv *jniEnv, jclass jclss, jlong cursor) {
    return (jint) (clang_getCursorLanguage(*(CXCursor*)cursor));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getTranslationUnit (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (clang_Cursor_getTranslationUnit(*(CXCursor*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1createCXCursorSet (JNIEnv *jniEnv, jclass jclss) {
    return (jlong) (clang_createCXCursorSet());
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1disposeCXCursorSet (JNIEnv *jniEnv, jclass jclss, jlong cset) {
    clang_disposeCXCursorSet((CXCursorSet)cset);
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1CXCursorSet_1contains (JNIEnv *jniEnv, jclass jclss, jlong cset, jlong cursor) {
    return (jint) (clang_CXCursorSet_contains((CXCursorSet)cset, *(CXCursor*)cursor));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1CXCursorSet_1insert (JNIEnv *jniEnv, jclass jclss, jlong cset, jlong cursor) {
    return (jint) (clang_CXCursorSet_insert((CXCursorSet)cset, *(CXCursor*)cursor));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorSemanticParent (JNIEnv *jniEnv, jclass jclss, jlong cursor, jlong retValPlacement) {
    *(CXCursor*)retValPlacement = clang_getCursorSemanticParent(*(CXCursor*)cursor);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorLexicalParent (JNIEnv *jniEnv, jclass jclss, jlong cursor, jlong retValPlacement) {
    *(CXCursor*)retValPlacement = clang_getCursorLexicalParent(*(CXCursor*)cursor);
    return (jlong) retValPlacement;
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1getOverriddenCursors (JNIEnv *jniEnv, jclass jclss, jlong cursor, jlong overridden, jlong num_overridden) {
    clang_getOverriddenCursors(*(CXCursor*)cursor, (CXCursor**)overridden, (unsigned int*)num_overridden);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1disposeOverriddenCursors (JNIEnv *jniEnv, jclass jclss, jlong overridden) {
    clang_disposeOverriddenCursors((CXCursor*)overridden);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getIncludedFile (JNIEnv *jniEnv, jclass jclss, jlong cursor) {
    return (jlong) (clang_getIncludedFile(*(CXCursor*)cursor));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursor (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1, jlong retValPlacement) {
    *(CXCursor*)retValPlacement = clang_getCursor((CXTranslationUnit)arg0, *(CXSourceLocation*)arg1);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorLocation (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXSourceLocation*)retValPlacement = clang_getCursorLocation(*(CXCursor*)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorExtent (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXSourceRange*)retValPlacement = clang_getCursorExtent(*(CXCursor*)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorType (JNIEnv *jniEnv, jclass jclss, jlong C, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_getCursorType(*(CXCursor*)C);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getTypeSpelling (JNIEnv *jniEnv, jclass jclss, jlong CT, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getTypeSpelling(*(CXType*)CT);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getTypedefDeclUnderlyingType (JNIEnv *jniEnv, jclass jclss, jlong C, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_getTypedefDeclUnderlyingType(*(CXCursor*)C);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getEnumDeclIntegerType (JNIEnv *jniEnv, jclass jclss, jlong C, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_getEnumDeclIntegerType(*(CXCursor*)C);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getEnumConstantDeclValue (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jlong) (clang_getEnumConstantDeclValue(*(CXCursor*)C));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getEnumConstantDeclUnsignedValue (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jlong) (clang_getEnumConstantDeclUnsignedValue(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getFieldDeclBitWidth (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_getFieldDeclBitWidth(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1getNumArguments (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_Cursor_getNumArguments(*(CXCursor*)C));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getArgument (JNIEnv *jniEnv, jclass jclss, jlong C, jint i, jlong retValPlacement) {
    *(CXCursor*)retValPlacement = clang_Cursor_getArgument(*(CXCursor*)C, (unsigned int)i);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1getNumTemplateArguments (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_Cursor_getNumTemplateArguments(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1getTemplateArgumentKind (JNIEnv *jniEnv, jclass jclss, jlong C, jint I) {
    return (jint) (clang_Cursor_getTemplateArgumentKind(*(CXCursor*)C, (unsigned int)I));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getTemplateArgumentType (JNIEnv *jniEnv, jclass jclss, jlong C, jint I, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_Cursor_getTemplateArgumentType(*(CXCursor*)C, (unsigned int)I);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getTemplateArgumentValue (JNIEnv *jniEnv, jclass jclss, jlong C, jint I) {
    return (jlong) (clang_Cursor_getTemplateArgumentValue(*(CXCursor*)C, (unsigned int)I));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getTemplateArgumentUnsignedValue (JNIEnv *jniEnv, jclass jclss, jlong C, jint I) {
    return (jlong) (clang_Cursor_getTemplateArgumentUnsignedValue(*(CXCursor*)C, (unsigned int)I));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1equalTypes (JNIEnv *jniEnv, jclass jclss, jlong A, jlong B) {
    return (jint) (clang_equalTypes(*(CXType*)A, *(CXType*)B));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCanonicalType (JNIEnv *jniEnv, jclass jclss, jlong T, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_getCanonicalType(*(CXType*)T);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isConstQualifiedType (JNIEnv *jniEnv, jclass jclss, jlong T) {
    return (jint) (clang_isConstQualifiedType(*(CXType*)T));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1isMacroFunctionLike (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_Cursor_isMacroFunctionLike(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1isMacroBuiltin (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_Cursor_isMacroBuiltin(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1isFunctionInlined (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_Cursor_isFunctionInlined(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isVolatileQualifiedType (JNIEnv *jniEnv, jclass jclss, jlong T) {
    return (jint) (clang_isVolatileQualifiedType(*(CXType*)T));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isRestrictQualifiedType (JNIEnv *jniEnv, jclass jclss, jlong T) {
    return (jint) (clang_isRestrictQualifiedType(*(CXType*)T));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getPointeeType (JNIEnv *jniEnv, jclass jclss, jlong T, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_getPointeeType(*(CXType*)T);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getTypeDeclaration (JNIEnv *jniEnv, jclass jclss, jlong T, jlong retValPlacement) {
    *(CXCursor*)retValPlacement = clang_getTypeDeclaration(*(CXType*)T);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getDeclObjCTypeEncoding (JNIEnv *jniEnv, jclass jclss, jlong C, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getDeclObjCTypeEncoding(*(CXCursor*)C);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Type_1getObjCEncoding (JNIEnv *jniEnv, jclass jclss, jlong type, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_Type_getObjCEncoding(*(CXType*)type);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getTypeKindSpelling (JNIEnv *jniEnv, jclass jclss, jint K, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getTypeKindSpelling((enum CXTypeKind)K);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getFunctionTypeCallingConv (JNIEnv *jniEnv, jclass jclss, jlong T) {
    return (jint) (clang_getFunctionTypeCallingConv(*(CXType*)T));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getResultType (JNIEnv *jniEnv, jclass jclss, jlong T, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_getResultType(*(CXType*)T);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getNumArgTypes (JNIEnv *jniEnv, jclass jclss, jlong T) {
    return (jint) (clang_getNumArgTypes(*(CXType*)T));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getArgType (JNIEnv *jniEnv, jclass jclss, jlong T, jint i, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_getArgType(*(CXType*)T, (unsigned int)i);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isFunctionTypeVariadic (JNIEnv *jniEnv, jclass jclss, jlong T) {
    return (jint) (clang_isFunctionTypeVariadic(*(CXType*)T));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorResultType (JNIEnv *jniEnv, jclass jclss, jlong C, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_getCursorResultType(*(CXCursor*)C);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isPODType (JNIEnv *jniEnv, jclass jclss, jlong T) {
    return (jint) (clang_isPODType(*(CXType*)T));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getElementType (JNIEnv *jniEnv, jclass jclss, jlong T, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_getElementType(*(CXType*)T);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getNumElements (JNIEnv *jniEnv, jclass jclss, jlong T) {
    return (jlong) (clang_getNumElements(*(CXType*)T));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getArrayElementType (JNIEnv *jniEnv, jclass jclss, jlong T, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_getArrayElementType(*(CXType*)T);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getArraySize (JNIEnv *jniEnv, jclass jclss, jlong T) {
    return (jlong) (clang_getArraySize(*(CXType*)T));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Type_1getNamedType (JNIEnv *jniEnv, jclass jclss, jlong T, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_Type_getNamedType(*(CXType*)T);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Type_1getAlignOf (JNIEnv *jniEnv, jclass jclss, jlong T) {
    return (jlong) (clang_Type_getAlignOf(*(CXType*)T));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Type_1getClassType (JNIEnv *jniEnv, jclass jclss, jlong T, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_Type_getClassType(*(CXType*)T);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Type_1getSizeOf (JNIEnv *jniEnv, jclass jclss, jlong T) {
    return (jlong) (clang_Type_getSizeOf(*(CXType*)T));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Type_1getOffsetOf (JNIEnv *jniEnv, jclass jclss, jlong T, jlong S) {
    return (jlong) (clang_Type_getOffsetOf(*(CXType*)T, (char*)S));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getOffsetOfField (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jlong) (clang_Cursor_getOffsetOfField(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1isAnonymous (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_Cursor_isAnonymous(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Type_1getNumTemplateArguments (JNIEnv *jniEnv, jclass jclss, jlong T) {
    return (jint) (clang_Type_getNumTemplateArguments(*(CXType*)T));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Type_1getTemplateArgumentAsType (JNIEnv *jniEnv, jclass jclss, jlong T, jint i, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_Type_getTemplateArgumentAsType(*(CXType*)T, (unsigned int)i);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Type_1getCXXRefQualifier (JNIEnv *jniEnv, jclass jclss, jlong T) {
    return (jint) (clang_Type_getCXXRefQualifier(*(CXType*)T));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1isBitField (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_Cursor_isBitField(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isVirtualBase (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jint) (clang_isVirtualBase(*(CXCursor*)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getCXXAccessSpecifier (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jint) (clang_getCXXAccessSpecifier(*(CXCursor*)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1getStorageClass (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jint) (clang_Cursor_getStorageClass(*(CXCursor*)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getNumOverloadedDecls (JNIEnv *jniEnv, jclass jclss, jlong cursor) {
    return (jint) (clang_getNumOverloadedDecls(*(CXCursor*)cursor));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getOverloadedDecl (JNIEnv *jniEnv, jclass jclss, jlong cursor, jint index, jlong retValPlacement) {
    *(CXCursor*)retValPlacement = clang_getOverloadedDecl(*(CXCursor*)cursor, (unsigned int)index);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getIBOutletCollectionType (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_getIBOutletCollectionType(*(CXCursor*)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1visitChildren (JNIEnv *jniEnv, jclass jclss, jlong parent, jlong visitor, jlong client_data) {
    return (jint) (clang_visitChildren(*(CXCursor*)parent, (CXCursorVisitor)visitor, (CXClientData)client_data));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorUSR (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getCursorUSR(*(CXCursor*)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1constructUSR_1ObjCClass (JNIEnv *jniEnv, jclass jclss, jlong class_name, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_constructUSR_ObjCClass((char*)class_name);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1constructUSR_1ObjCCategory (JNIEnv *jniEnv, jclass jclss, jlong class_name, jlong category_name, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_constructUSR_ObjCCategory((char*)class_name, (char*)category_name);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1constructUSR_1ObjCProtocol (JNIEnv *jniEnv, jclass jclss, jlong protocol_name, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_constructUSR_ObjCProtocol((char*)protocol_name);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1constructUSR_1ObjCIvar (JNIEnv *jniEnv, jclass jclss, jlong name, jlong classUSR, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_constructUSR_ObjCIvar((char*)name, *(CXString*)classUSR);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1constructUSR_1ObjCMethod (JNIEnv *jniEnv, jclass jclss, jlong name, jint isInstanceMethod, jlong classUSR, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_constructUSR_ObjCMethod((char*)name, (unsigned int)isInstanceMethod, *(CXString*)classUSR);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1constructUSR_1ObjCProperty (JNIEnv *jniEnv, jclass jclss, jlong property, jlong classUSR, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_constructUSR_ObjCProperty((char*)property, *(CXString*)classUSR);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorSpelling (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getCursorSpelling(*(CXCursor*)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getSpellingNameRange (JNIEnv *jniEnv, jclass jclss, jlong arg0, jint pieceIndex, jint options, jlong retValPlacement) {
    *(CXSourceRange*)retValPlacement = clang_Cursor_getSpellingNameRange(*(CXCursor*)arg0, (unsigned int)pieceIndex, (unsigned int)options);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorDisplayName (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getCursorDisplayName(*(CXCursor*)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorReferenced (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXCursor*)retValPlacement = clang_getCursorReferenced(*(CXCursor*)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorDefinition (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXCursor*)retValPlacement = clang_getCursorDefinition(*(CXCursor*)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1isCursorDefinition (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jint) (clang_isCursorDefinition(*(CXCursor*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCanonicalCursor (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXCursor*)retValPlacement = clang_getCanonicalCursor(*(CXCursor*)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1getObjCSelectorIndex (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jint) (clang_Cursor_getObjCSelectorIndex(*(CXCursor*)arg0));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1isDynamicCall (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_Cursor_isDynamicCall(*(CXCursor*)C));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getReceiverType (JNIEnv *jniEnv, jclass jclss, jlong C, jlong retValPlacement) {
    *(CXType*)retValPlacement = clang_Cursor_getReceiverType(*(CXCursor*)C);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1getObjCPropertyAttributes (JNIEnv *jniEnv, jclass jclss, jlong C, jint reserved) {
    return (jint) (clang_Cursor_getObjCPropertyAttributes(*(CXCursor*)C, (unsigned int)reserved));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1getObjCDeclQualifiers (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_Cursor_getObjCDeclQualifiers(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1isObjCOptional (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_Cursor_isObjCOptional(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Cursor_1isVariadic (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_Cursor_isVariadic(*(CXCursor*)C));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getCommentRange (JNIEnv *jniEnv, jclass jclss, jlong C, jlong retValPlacement) {
    *(CXSourceRange*)retValPlacement = clang_Cursor_getCommentRange(*(CXCursor*)C);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getRawCommentText (JNIEnv *jniEnv, jclass jclss, jlong C, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_Cursor_getRawCommentText(*(CXCursor*)C);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getBriefCommentText (JNIEnv *jniEnv, jclass jclss, jlong C, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_Cursor_getBriefCommentText(*(CXCursor*)C);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getMangling (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_Cursor_getMangling(*(CXCursor*)arg0);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getCXXManglings (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (clang_Cursor_getCXXManglings(*(CXCursor*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1getModule (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jlong) (clang_Cursor_getModule(*(CXCursor*)C));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getModuleForFile (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1) {
    return (jlong) (clang_getModuleForFile((CXTranslationUnit)arg0, (CXFile)arg1));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Module_1getASTFile (JNIEnv *jniEnv, jclass jclss, jlong Module) {
    return (jlong) (clang_Module_getASTFile((CXModule)Module));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Module_1getParent (JNIEnv *jniEnv, jclass jclss, jlong Module) {
    return (jlong) (clang_Module_getParent((CXModule)Module));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Module_1getName (JNIEnv *jniEnv, jclass jclss, jlong Module, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_Module_getName((CXModule)Module);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Module_1getFullName (JNIEnv *jniEnv, jclass jclss, jlong Module, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_Module_getFullName((CXModule)Module);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Module_1isSystem (JNIEnv *jniEnv, jclass jclss, jlong Module) {
    return (jint) (clang_Module_isSystem((CXModule)Module));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Module_1getNumTopLevelHeaders (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong Module) {
    return (jint) (clang_Module_getNumTopLevelHeaders((CXTranslationUnit)arg0, (CXModule)Module));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Module_1getTopLevelHeader (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong Module, jint Index) {
    return (jlong) (clang_Module_getTopLevelHeader((CXTranslationUnit)arg0, (CXModule)Module, (unsigned int)Index));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1CXXConstructor_1isConvertingConstructor (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_CXXConstructor_isConvertingConstructor(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1CXXConstructor_1isCopyConstructor (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_CXXConstructor_isCopyConstructor(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1CXXConstructor_1isDefaultConstructor (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_CXXConstructor_isDefaultConstructor(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1CXXConstructor_1isMoveConstructor (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_CXXConstructor_isMoveConstructor(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1CXXField_1isMutable (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_CXXField_isMutable(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1CXXMethod_1isDefaulted (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_CXXMethod_isDefaulted(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1CXXMethod_1isPureVirtual (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_CXXMethod_isPureVirtual(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1CXXMethod_1isStatic (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_CXXMethod_isStatic(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1CXXMethod_1isVirtual (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_CXXMethod_isVirtual(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1CXXMethod_1isConst (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_CXXMethod_isConst(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getTemplateCursorKind (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jint) (clang_getTemplateCursorKind(*(CXCursor*)C));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getSpecializedCursorTemplate (JNIEnv *jniEnv, jclass jclss, jlong C, jlong retValPlacement) {
    *(CXCursor*)retValPlacement = clang_getSpecializedCursorTemplate(*(CXCursor*)C);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorReferenceNameRange (JNIEnv *jniEnv, jclass jclss, jlong C, jint NameFlags, jint PieceIndex, jlong retValPlacement) {
    *(CXSourceRange*)retValPlacement = clang_getCursorReferenceNameRange(*(CXCursor*)C, (unsigned int)NameFlags, (unsigned int)PieceIndex);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getTokenKind (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jint) (clang_getTokenKind(*(CXToken*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getTokenSpelling (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getTokenSpelling((CXTranslationUnit)arg0, *(CXToken*)arg1);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getTokenLocation (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1, jlong retValPlacement) {
    *(CXSourceLocation*)retValPlacement = clang_getTokenLocation((CXTranslationUnit)arg0, *(CXToken*)arg1);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getTokenExtent (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1, jlong retValPlacement) {
    *(CXSourceRange*)retValPlacement = clang_getTokenExtent((CXTranslationUnit)arg0, *(CXToken*)arg1);
    return (jlong) retValPlacement;
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1tokenize (JNIEnv *jniEnv, jclass jclss, jlong TU, jlong Range, jlong Tokens, jlong NumTokens) {
    clang_tokenize((CXTranslationUnit)TU, *(CXSourceRange*)Range, (CXToken**)Tokens, (unsigned int*)NumTokens);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1annotateTokens (JNIEnv *jniEnv, jclass jclss, jlong TU, jlong Tokens, jint NumTokens, jlong Cursors) {
    clang_annotateTokens((CXTranslationUnit)TU, (CXToken*)Tokens, (unsigned int)NumTokens, (CXCursor*)Cursors);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1disposeTokens (JNIEnv *jniEnv, jclass jclss, jlong TU, jlong Tokens, jint NumTokens) {
    clang_disposeTokens((CXTranslationUnit)TU, (CXToken*)Tokens, (unsigned int)NumTokens);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorKindSpelling (JNIEnv *jniEnv, jclass jclss, jint Kind, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getCursorKindSpelling((enum CXCursorKind)Kind);
    return (jlong) retValPlacement;
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1getDefinitionSpellingAndExtent (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong startBuf, jlong endBuf, jlong startLine, jlong startColumn, jlong endLine, jlong endColumn) {
    clang_getDefinitionSpellingAndExtent(*(CXCursor*)arg0, (char**)startBuf, (char**)endBuf, (unsigned int*)startLine, (unsigned int*)startColumn, (unsigned int*)endLine, (unsigned int*)endColumn);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1enableStackTraces (JNIEnv *jniEnv, jclass jclss) {
    clang_enableStackTraces();
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1executeOnThread (JNIEnv *jniEnv, jclass jclss, jlong fn, jlong user_data, jint stack_size) {
    clang_executeOnThread((void*)fn, (void*)user_data, (unsigned int)stack_size);
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getCompletionChunkKind (JNIEnv *jniEnv, jclass jclss, jlong completion_string, jint chunk_number) {
    return (jint) (clang_getCompletionChunkKind((CXCompletionString)completion_string, (unsigned int)chunk_number));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCompletionChunkText (JNIEnv *jniEnv, jclass jclss, jlong completion_string, jint chunk_number, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getCompletionChunkText((CXCompletionString)completion_string, (unsigned int)chunk_number);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCompletionChunkCompletionString (JNIEnv *jniEnv, jclass jclss, jlong completion_string, jint chunk_number) {
    return (jlong) (clang_getCompletionChunkCompletionString((CXCompletionString)completion_string, (unsigned int)chunk_number));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getNumCompletionChunks (JNIEnv *jniEnv, jclass jclss, jlong completion_string) {
    return (jint) (clang_getNumCompletionChunks((CXCompletionString)completion_string));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getCompletionPriority (JNIEnv *jniEnv, jclass jclss, jlong completion_string) {
    return (jint) (clang_getCompletionPriority((CXCompletionString)completion_string));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getCompletionAvailability (JNIEnv *jniEnv, jclass jclss, jlong completion_string) {
    return (jint) (clang_getCompletionAvailability((CXCompletionString)completion_string));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1getCompletionNumAnnotations (JNIEnv *jniEnv, jclass jclss, jlong completion_string) {
    return (jint) (clang_getCompletionNumAnnotations((CXCompletionString)completion_string));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCompletionAnnotation (JNIEnv *jniEnv, jclass jclss, jlong completion_string, jint annotation_number, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getCompletionAnnotation((CXCompletionString)completion_string, (unsigned int)annotation_number);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCompletionParent (JNIEnv *jniEnv, jclass jclss, jlong completion_string, jlong kind, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getCompletionParent((CXCompletionString)completion_string, (enum CXCursorKind*)kind);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCompletionBriefComment (JNIEnv *jniEnv, jclass jclss, jlong completion_string, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getCompletionBriefComment((CXCompletionString)completion_string);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getCursorCompletionString (JNIEnv *jniEnv, jclass jclss, jlong cursor) {
    return (jlong) (clang_getCursorCompletionString(*(CXCursor*)cursor));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1defaultCodeCompleteOptions (JNIEnv *jniEnv, jclass jclss) {
    return (jint) (clang_defaultCodeCompleteOptions());
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1codeCompleteAt (JNIEnv *jniEnv, jclass jclss, jlong TU, jlong complete_filename, jint complete_line, jint complete_column, jlong unsaved_files, jint num_unsaved_files, jint options) {
    return (jlong) (clang_codeCompleteAt((CXTranslationUnit)TU, (char*)complete_filename, (unsigned int)complete_line, (unsigned int)complete_column, (struct CXUnsavedFile*)unsaved_files, (unsigned int)num_unsaved_files, (unsigned int)options));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1sortCodeCompletionResults (JNIEnv *jniEnv, jclass jclss, jlong Results, jint NumResults) {
    clang_sortCodeCompletionResults((CXCompletionResult*)Results, (unsigned int)NumResults);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1disposeCodeCompleteResults (JNIEnv *jniEnv, jclass jclss, jlong Results) {
    clang_disposeCodeCompleteResults((CXCodeCompleteResults*)Results);
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1codeCompleteGetNumDiagnostics (JNIEnv *jniEnv, jclass jclss, jlong Results) {
    return (jint) (clang_codeCompleteGetNumDiagnostics((CXCodeCompleteResults*)Results));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1codeCompleteGetDiagnostic (JNIEnv *jniEnv, jclass jclss, jlong Results, jint Index) {
    return (jlong) (clang_codeCompleteGetDiagnostic((CXCodeCompleteResults*)Results, (unsigned int)Index));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1codeCompleteGetContexts (JNIEnv *jniEnv, jclass jclss, jlong Results) {
    return (jlong) (clang_codeCompleteGetContexts((CXCodeCompleteResults*)Results));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1codeCompleteGetContainerKind (JNIEnv *jniEnv, jclass jclss, jlong Results, jlong IsIncomplete) {
    return (jint) (clang_codeCompleteGetContainerKind((CXCodeCompleteResults*)Results, (unsigned int*)IsIncomplete));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1codeCompleteGetContainerUSR (JNIEnv *jniEnv, jclass jclss, jlong Results, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_codeCompleteGetContainerUSR((CXCodeCompleteResults*)Results);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1codeCompleteGetObjCSelector (JNIEnv *jniEnv, jclass jclss, jlong Results, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_codeCompleteGetObjCSelector((CXCodeCompleteResults*)Results);
    return (jlong) retValPlacement;
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getClangVersion (JNIEnv *jniEnv, jclass jclss, jlong retValPlacement) {
    *(CXString*)retValPlacement = clang_getClangVersion();
    return (jlong) retValPlacement;
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1toggleCrashRecovery (JNIEnv *jniEnv, jclass jclss, jint isEnabled) {
    clang_toggleCrashRecovery((unsigned int)isEnabled);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1getInclusions (JNIEnv *jniEnv, jclass jclss, jlong tu, jlong visitor, jlong client_data) {
    clang_getInclusions((CXTranslationUnit)tu, (CXInclusionVisitor)visitor, (CXClientData)client_data);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1Cursor_1Evaluate (JNIEnv *jniEnv, jclass jclss, jlong C) {
    return (jlong) (clang_Cursor_Evaluate(*(CXCursor*)C));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1EvalResult_1getKind (JNIEnv *jniEnv, jclass jclss, jlong E) {
    return (jint) (clang_EvalResult_getKind((CXEvalResult)E));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1EvalResult_1getAsInt (JNIEnv *jniEnv, jclass jclss, jlong E) {
    return (jint) (clang_EvalResult_getAsInt((CXEvalResult)E));
}

JNIEXPORT jdouble JNICALL Java_clang_clang_kni_1clang_1EvalResult_1getAsDouble (JNIEnv *jniEnv, jclass jclss, jlong E) {
    return (jdouble) (clang_EvalResult_getAsDouble((CXEvalResult)E));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1EvalResult_1getAsStr (JNIEnv *jniEnv, jclass jclss, jlong E) {
    return (jlong) (clang_EvalResult_getAsStr((CXEvalResult)E));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1EvalResult_1dispose (JNIEnv *jniEnv, jclass jclss, jlong E) {
    clang_EvalResult_dispose((CXEvalResult)E);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getRemappings (JNIEnv *jniEnv, jclass jclss, jlong path) {
    return (jlong) (clang_getRemappings((char*)path));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1getRemappingsFromFileList (JNIEnv *jniEnv, jclass jclss, jlong filePaths, jint numFiles) {
    return (jlong) (clang_getRemappingsFromFileList((char**)filePaths, (unsigned int)numFiles));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1remap_1getNumFiles (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jint) (clang_remap_getNumFiles((CXRemapping)arg0));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1remap_1getFilenames (JNIEnv *jniEnv, jclass jclss, jlong arg0, jint index, jlong original, jlong transformed) {
    clang_remap_getFilenames((CXRemapping)arg0, (unsigned int)index, (CXString*)original, (CXString*)transformed);
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1remap_1dispose (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    clang_remap_dispose((CXRemapping)arg0);
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1findReferencesInFile (JNIEnv *jniEnv, jclass jclss, jlong cursor, jlong file, jlong visitor) {
    return (jint) (clang_findReferencesInFile(*(CXCursor*)cursor, (CXFile)file, *(struct CXCursorAndRangeVisitor*)visitor));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1findIncludesInFile (JNIEnv *jniEnv, jclass jclss, jlong TU, jlong file, jlong visitor) {
    return (jint) (clang_findIncludesInFile((CXTranslationUnit)TU, (CXFile)file, *(struct CXCursorAndRangeVisitor*)visitor));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1index_1isEntityObjCContainerKind (JNIEnv *jniEnv, jclass jclss, jint arg0) {
    return (jint) (clang_index_isEntityObjCContainerKind((CXIdxEntityKind)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1index_1getObjCContainerDeclInfo (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (clang_index_getObjCContainerDeclInfo((CXIdxDeclInfo*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1index_1getObjCInterfaceDeclInfo (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (clang_index_getObjCInterfaceDeclInfo((CXIdxDeclInfo*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1index_1getObjCCategoryDeclInfo (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (clang_index_getObjCCategoryDeclInfo((CXIdxDeclInfo*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1index_1getObjCProtocolRefListInfo (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (clang_index_getObjCProtocolRefListInfo((CXIdxDeclInfo*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1index_1getObjCPropertyDeclInfo (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (clang_index_getObjCPropertyDeclInfo((CXIdxDeclInfo*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1index_1getIBOutletCollectionAttrInfo (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (clang_index_getIBOutletCollectionAttrInfo((CXIdxAttrInfo*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1index_1getCXXClassDeclInfo (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (clang_index_getCXXClassDeclInfo((CXIdxDeclInfo*)arg0));
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1index_1getClientContainer (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (clang_index_getClientContainer((CXIdxContainerInfo*)arg0));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1index_1setClientContainer (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1) {
    clang_index_setClientContainer((CXIdxContainerInfo*)arg0, (CXIdxClientContainer)arg1);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1index_1getClientEntity (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    return (jlong) (clang_index_getClientEntity((CXIdxEntityInfo*)arg0));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1index_1setClientEntity (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong arg1) {
    clang_index_setClientEntity((CXIdxEntityInfo*)arg0, (CXIdxClientEntity)arg1);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1IndexAction_1create (JNIEnv *jniEnv, jclass jclss, jlong CIdx) {
    return (jlong) (clang_IndexAction_create((CXIndex)CIdx));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1IndexAction_1dispose (JNIEnv *jniEnv, jclass jclss, jlong arg0) {
    clang_IndexAction_dispose((CXIndexAction)arg0);
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1indexSourceFile (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong client_data, jlong index_callbacks, jint index_callbacks_size, jint index_options, jlong source_filename, jlong command_line_args, jint num_command_line_args, jlong unsaved_files, jint num_unsaved_files, jlong out_TU, jint TU_options) {
    return (jint) (clang_indexSourceFile((CXIndexAction)arg0, (CXClientData)client_data, (IndexerCallbacks*)index_callbacks, (unsigned int)index_callbacks_size, (unsigned int)index_options, (char*)source_filename, (char**)command_line_args, (int)num_command_line_args, (struct CXUnsavedFile*)unsaved_files, (unsigned int)num_unsaved_files, (CXTranslationUnit*)out_TU, (unsigned int)TU_options));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1indexSourceFileFullArgv (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong client_data, jlong index_callbacks, jint index_callbacks_size, jint index_options, jlong source_filename, jlong command_line_args, jint num_command_line_args, jlong unsaved_files, jint num_unsaved_files, jlong out_TU, jint TU_options) {
    return (jint) (clang_indexSourceFileFullArgv((CXIndexAction)arg0, (CXClientData)client_data, (IndexerCallbacks*)index_callbacks, (unsigned int)index_callbacks_size, (unsigned int)index_options, (char*)source_filename, (char**)command_line_args, (int)num_command_line_args, (struct CXUnsavedFile*)unsaved_files, (unsigned int)num_unsaved_files, (CXTranslationUnit*)out_TU, (unsigned int)TU_options));
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1indexTranslationUnit (JNIEnv *jniEnv, jclass jclss, jlong arg0, jlong client_data, jlong index_callbacks, jint index_callbacks_size, jint index_options, jlong arg5) {
    return (jint) (clang_indexTranslationUnit((CXIndexAction)arg0, (CXClientData)client_data, (IndexerCallbacks*)index_callbacks, (unsigned int)index_callbacks_size, (unsigned int)index_options, (CXTranslationUnit)arg5));
}

JNIEXPORT void JNICALL Java_clang_clang_kni_1clang_1indexLoc_1getFileLocation (JNIEnv *jniEnv, jclass jclss, jlong loc, jlong indexFile, jlong file, jlong line, jlong column, jlong offset) {
    clang_indexLoc_getFileLocation(*(CXIdxLoc*)loc, (CXIdxClientFile*)indexFile, (CXFile*)file, (unsigned int*)line, (unsigned int*)column, (unsigned int*)offset);
}

JNIEXPORT jlong JNICALL Java_clang_clang_kni_1clang_1indexLoc_1getCXSourceLocation (JNIEnv *jniEnv, jclass jclss, jlong loc, jlong retValPlacement) {
    *(CXSourceLocation*)retValPlacement = clang_indexLoc_getCXSourceLocation(*(CXIdxLoc*)loc);
    return (jlong) retValPlacement;
}

JNIEXPORT jint JNICALL Java_clang_clang_kni_1clang_1Type_1visitFields (JNIEnv *jniEnv, jclass jclss, jlong T, jlong visitor, jlong client_data) {
    return (jint) (clang_Type_visitFields(*(CXType*)T, (CXFieldVisitor)visitor, (CXClientData)client_data));
}

