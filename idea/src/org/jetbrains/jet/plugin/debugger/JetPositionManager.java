/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.debugger;

import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.util.*;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jna.TypeMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.NamespaceCodegen;
import org.jetbrains.jet.di.InjectorForJetTypeMapper;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

import java.util.*;

/**
 * @author yole
 * @author alex.tkachman
 */
public class JetPositionManager implements PositionManager {
    private final DebugProcess myDebugProcess;
    private WeakHashMap<FqName, CachedValue<JetTypeMapper>> myTypeMappers = new WeakHashMap<FqName, CachedValue<JetTypeMapper>>();

    public JetPositionManager(DebugProcess debugProcess) {
        myDebugProcess = debugProcess;
    }

    @Override
    @Nullable
    public SourcePosition getSourcePosition(@Nullable Location location) throws NoDataException {
        if (location == null) {
            throw new NoDataException();
        }
        PsiFile psiFile = getPsiFileByLocation(location);

        int lineNumber;
        try {
            lineNumber = location.lineNumber() - 1;
        }
        catch (InternalError e) {
            lineNumber = -1;
        }

        if (lineNumber >= 0) {
            return SourcePosition.createFromLine(psiFile, lineNumber);
        }

        throw new NoDataException();
    }

    private PsiFile getPsiFileByLocation(Location location) throws NoDataException {
        try {
            final String sourceName = location.sourceName();
            final Project project = myDebugProcess.getProject();
            final PsiFile[] files = FilenameIndex.getFilesByName(project, sourceName, ProjectScope.getAllScope(project));
            if (files.length == 1 && files[0] instanceof JetFile) {
                return files[0];
            }
        }
        catch (AbsentInformationException e) {
            throw new NoDataException();
        }


        /* TODO

        final ReferenceType referenceType = location.declaringType();
        if (referenceType == null) {
            return null;
        }

        // TODO
        return null;
        */
        throw new NoDataException();
    }

    @NotNull
    @Override
    public List<ReferenceType> getAllClasses(SourcePosition sourcePosition) throws NoDataException {
        if (!(sourcePosition.getFile() instanceof JetFile)) {
            throw new NoDataException();
        }
        final Collection<String> names = classNamesForPosition(sourcePosition);
        List<ReferenceType> result = new ArrayList<ReferenceType>();
        for (String name : names) {
            result.addAll(myDebugProcess.getVirtualMachineProxy().classesByName(name));
        }
        return result;
    }

    private Collection<String> classNamesForPosition(final SourcePosition sourcePosition) {

        final Collection<String> names = new ArrayList<String>();

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                final JetFile file = (JetFile)sourcePosition.getFile();
                JetTypeMapper typeMapper = prepareTypeMapper(file);

                PsiElement psiElement = PsiTreeUtil.getParentOfType(sourcePosition.getElementAt(), JetClassOrObject.class, JetFunctionLiteralExpression.class, JetNamedFunction.class);
                if (psiElement == null) {
                    JetFile namespace = PsiTreeUtil.getParentOfType(sourcePosition.getElementAt(), JetFile.class);
                    if (namespace != null) {
                        names.add(NamespaceCodegen.getJVMClassNameForKotlinNs(JetPsiUtil.getFQName(namespace)).getInternalName());
                    }
                    else {
                        names.add(NamespaceCodegen.getJVMClassNameForKotlinNs(JetPsiUtil.getFQName(file)).getInternalName());
                    }
                }
                else {
                    if (psiElement instanceof JetClassOrObject) {
                        names.addAll(typeMapper.allJvmNames((JetClassOrObject) psiElement));
                    }
                    else if(psiElement instanceof JetNamedFunction) {
                        if(psiElement.getParent() instanceof JetFile) {
                            JetFile namespace = PsiTreeUtil.getParentOfType(sourcePosition.getElementAt(), JetFile.class);
                            boolean multiFileNamespace = typeMapper.getClosureAnnotator().isMultiFileNamespace(JetPsiUtil.getFQName(namespace));
                            if(multiFileNamespace) {
                                String name = namespace.getName();
                                names.add(NamespaceCodegen.getJVMClassNameForKotlinNs(JetPsiUtil.getFQName(namespace)).getInternalName() + "$src$" + name.substring(0,name.lastIndexOf('.')));
                            }
                            else {
                                names.add(NamespaceCodegen.getJVMClassNameForKotlinNs(JetPsiUtil.getFQName(namespace)).getInternalName());
                            }
                        }
                        else {
                            JetFile namespace = PsiTreeUtil.getParentOfType(sourcePosition.getElementAt(), JetFile.class);
                            if (namespace != null) {
                                names.add(NamespaceCodegen.getJVMClassNameForKotlinNs(JetPsiUtil.getFQName(namespace)).getInternalName());
                            }
                            else {
                                names.add(NamespaceCodegen.getJVMClassNameForKotlinNs(JetPsiUtil.getFQName(file)).getInternalName());
                            }
                        }
                    }
                    else {
                        names.add(typeMapper.getClosureAnnotator().classNameForAnonymousClass((JetElement) psiElement).getInternalName());
                    }
                }
            }
        });

        return names;
    }

    private JetTypeMapper prepareTypeMapper(final JetFile file) {
        FqName fqName = JetPsiUtil.getFQName(file);
        CachedValue<JetTypeMapper> value = myTypeMappers.get(fqName);
        if(value == null) {
            value = CachedValuesManager.getManager(file.getProject()).createCachedValue(new CachedValueProvider<JetTypeMapper>() {
                @Override
                public Result<JetTypeMapper> compute() {
                    final AnalyzeExhaust analyzeExhaust = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file);
                    analyzeExhaust.throwIfError();

                    List<JetFile> namespaceFiles = JetFilesProvider.getInstance(file.getProject()).allNamespaceFiles().fun(file);

                    JetTypeMapper typeMapper = new InjectorForJetTypeMapper(analyzeExhaust.getBindingContext(), namespaceFiles).getJetTypeMapper();
                    typeMapper.getClosureAnnotator().init();
                    return new Result<JetTypeMapper>(typeMapper, PsiModificationTracker.MODIFICATION_COUNT);
                }
            }, false);
            myTypeMappers.put(fqName, value);
        }

        return value.getValue();
    }

    @NotNull
    @Override
    public List<Location> locationsOfLine(ReferenceType type, SourcePosition position) throws NoDataException {
        if (!(position.getFile() instanceof JetFile)) {
            throw new NoDataException();
        }
        try {
            int line = position.getLine() + 1;
            List<Location> locations = myDebugProcess.getVirtualMachineProxy().versionHigher("1.4")
                                       ? type.locationsOfLine(DebugProcess.JAVA_STRATUM, null, line)
                                       : type.locationsOfLine(line);
            if (locations == null || locations.isEmpty()) throw new NoDataException();
            return locations;
        }
        catch (AbsentInformationException e) {
            throw new NoDataException();
        }
    }

    @Override
    public ClassPrepareRequest createPrepareRequest(ClassPrepareRequestor classPrepareRequestor,
                                                    SourcePosition sourcePosition) throws NoDataException {
        if (!(sourcePosition.getFile() instanceof JetFile)) {
            throw new NoDataException();
        }
        final Collection<String> classNames = classNamesForPosition(sourcePosition);
        if (classNames.isEmpty()) {
            return null;
        }
        final Iterator<String> iterator = classNames.iterator();
        boolean wildcard = false;
        String namePattern = iterator.next();
        while (iterator.hasNext()) {
            namePattern = StringUtil.commonPrefix(namePattern, iterator.next());
            wildcard = true;
        }
        if (wildcard) {
            namePattern += "*";
        }
        return myDebugProcess.getRequestsManager().createClassPrepareRequest(classPrepareRequestor, namePattern.replace('/', '.'));
    }
}
