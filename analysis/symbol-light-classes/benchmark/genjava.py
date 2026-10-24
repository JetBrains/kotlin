"""Generates Java files that use the API of the synthetic Kotlin code base produced by gen.py.

usage: genjava.py <files> <out-dir>
Writes <out-dir>/javause/UseN.java: calls of methods, accessors, constructors, companion and object members, facades,
plus a subclass of the base service and an implementation of the repository interface.
"""
import os, sys
n = int(sys.argv[1]); out = sys.argv[2]
os.makedirs(os.path.join(out, 'javause'), exist_ok=True)
for i in range(n):
    p = f"bench.p{i}"
    tag_list = f"""
        TagList{i} tagList = new TagList{i}(tags);
        int tagCount = tagList.size() + tagList.get(0).length();
        for (String t : tagList) {{ total += t.length(); }}""" if i % 5 == 0 else ""
    delegation = f"""
        LoggingRepo{i} logging = new LoggingRepo{i}(repo);
        logging.save(dto);
        total += logging.getSize() + logging.count();""" if i % 4 == 0 else ""
    src = f"""package javause;

import {p}.*;
import java.util.*;

public class Use{i} {{
    public int use(Service{i} service, Dto{i} dto, Repo{i}<Dto{i}, Long> repo) throws Exception {{
        int total = 0;
        String name = dto.getName();
        long id = dto.getId();
        List<String> tags = dto.getTags();
        Map<String, Object> attributes = dto.getAttributes();
        total += dto.getCounter();
        dto.setCounter(total);
        total += dto.getDisplayName().length() + dto.withName("x").getName().length();
        Dto{i} copy = dto.copy(id, name, tags, attributes, null, 1);
        total += copy.component2().length() + copy.hashCode();
        if (copy.equals(dto)) total++;
        total += new Dto{i}(1L, "n", tags, attributes, null, 0).getName().length();

        total += service.find("a").size() + service.find("a", 1).size() + service.find("a", 1, true).size();
        service.log(1, "a", "b", "c");
        total += service.sortedBy(d -> d.getName()).size();
        Dto{i} legacy = service.legacy();
        total += service.renamedForJava() + (service.annotated("s") == null ? 0 : 1);
        total += service.getStats().size() + service.getMode().length() + (int) service.getLastRun();
        service.setLastRun(1L);
        service.setCache(new HashMap<>());
        total += service.getCache().size();
        total += service.compareTo(service);
        if (service.validate(dto)) total++;
        total += service.process(tags.isEmpty() ? Collections.<Dto{i}>emptyList() : Collections.singletonList(dto), d -> true).size();
        Service{i}.Session session = service.new Session("token");
        total += session.getToken().length() + session.service().hashCode();
        Service{i}.Config config = new Service{i}.Config(1, false);
        total += config.getLimit() + (config.getEnabled() ? 1 : 0);
        total += Service{i}.VERSION + Service{i}.DEFAULT_LIMIT;
        Service{i} created = Service{i}.create(repo);
        Service{i} createdViaCompanion = Service{i}.Companion.create(repo);
        total += created.hashCode() + createdViaCompanion.hashCode();
        List<Dto{i}> target = new ArrayList<>();
        service.copyInto(target, target);
        AutoCloseable subscription = service.subscribe((d, continuation) -> null);
        subscription.close();
        Status{i} status = service.status(dto);
        total += status.getCode() + status.label().length() + status.ordinal() + Status{i}.valueOf("ACTIVE").getCode();
        total += Status{i}.values().length + Status{i}.getEntries().size();
        Status{i} byCode = Status{i}.Companion.byCode(2);
        switch (status) {{
            case ACTIVE: total++; break;
            case SUSPENDED: total += 2; break;
            default: break;
        }}

        Result{i}<Dto{i}> result = new Result{i}.Ok<>(dto);
        if (result instanceof Result{i}.Ok) total += ((Result{i}.Ok<Dto{i}>) result).getValue().getName().length();
        Result{i}.Err error = new Result{i}.Err(new RuntimeException(), true);
        total += error.getRetryable() ? 1 : 0;
        Object loading = Result{i}.Loading.INSTANCE;

        Registry{i}.INSTANCE.register(dto);
        total += Registry{i}.INSTANCE.getCount();
        Registry{i}.reset();

        total += repo.findAll(10, 0).size() + repo.count() + repo.getSize();
        Dto{i} found = repo.findById(1L);
        if (repo.delete(dto)) total++;

        total += Utils{i}Kt.display(dto).length();
        if (Utils{i}Kt.isRoot(dto)) total++;
        String second = Utils{i}Kt.secondOrNull{i}(tags);
        Dto{i} createdDto = Utils{i}Kt.createDto{i}(1L, "dto", "a", "b");
        total += Utils{i}Kt.MAX_ITEMS_{i} + Utils{i}Kt.getDefaultTags{i}().size();
        Utils{i}Kt.setMutableSetting{i}("x");
        total += Utils{i}Kt.getMutableSetting{i}() == null ? 0 : 1;
{tag_list}{delegation}
        return total;
    }}

    public static class MyService{i} extends BaseService{i}<Dto{i}> {{
        public MyService{i}(Repo{i}<Dto{i}, Long> repo) {{
            super(repo);
        }}

        @Override
        public boolean validate(Dto{i} entity) {{
            return entity.getName().isEmpty();
        }}

        @Override
        public List<Dto{i}> process(Collection<? extends Dto{i}> items, kotlin.jvm.functions.Function1<? super Dto{i}, Boolean> filter) {{
            return super.process(items, filter);
        }}

        @Override
        protected String getServiceName() {{
            return "my";
        }}
    }}

    public static class MyRepo{i} implements Repo{i}<Dto{i}, Long> {{
        @Override
        public Dto{i} findById(Long id) {{ return null; }}

        @Override
        public List<Dto{i}> findAll(int limit, int offset) {{ return Collections.emptyList(); }}

        @Override
        public Dto{i} save(Dto{i} entity) {{ return entity; }}

        @Override
        public boolean delete(Dto{i} entity) {{ return false; }}

        @Override
        public int getSize() {{ return 0; }}

        @Override
        public int count() {{ return 0; }}
    }}
}}
"""
    with open(os.path.join(out, 'javause', f'Use{i}.java'), 'w') as f:
        f.write(src)
